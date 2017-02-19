package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDevice;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchImpl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.UltrasonicSensor;

@Autonomous(name="TwoChainzAutoRedV2", group="Walbots")
public class TwoChainzRedAutonomousV2OpMode extends OpMode
{
    // software-hardware proxy object variables

    DcMotor motorLeftFrontWheels;
    DcMotor motorLeftRearWheels;
    DcMotor motorRightFrontWheels;
    DcMotor motorRightRearWheels;
    DcMotor motorAltitude;
    DcMotor motorRotate;
    DcMotor motorLaunchLeft;
    DcMotor motorLaunchRight;
    ColorSensor colorSensor;
    Servo   clawServoLeft;
    Servo   clawServoRight;
    CRServo triggerServo;

    // timer & state variables

    boolean startingAutonomous;
    boolean barrelRaising;
    double  spinUpTime;
    double  buttonPressTime1;
    double  buttonPressTime2;
    double  buttonRetractTime1;
    double  buttonRetractTime2;
    double  rotateTime3;
    double  triggerTime;
    double  driveTime3;
    double  spacingTime1;
    double  stopTime1;
    boolean buttonSensor1;
    boolean buttonSensor2;
    double  driveTime4;
    // range sensor, taken from: http://www.modernroboticsinc.com/range-sensor

    byte[] rangeAcache;
    byte[] rangeCcache;

    I2cDevice rangeA;
    I2cDevice rangeC;
    I2cDeviceSynch rangeAreader;
    I2cDeviceSynch rangeCreader;

    // constants to tweak certain movement
    static public final double STOW             = 1f;
    static public final int    ALTITUDE_FIRE    = -1500;
    static public final double ENCODER_POWER    = 0.75f;
    static public final double LAUNCH_POWER     = 0.15f;
    static public final double TRIGGER_POWER    = 0.5f;
    static public final int    RED_CUT_OFF      = 1;
    static public final int    BLUE             = 1;

    // constants to use for timer intervals

    static public final double INTERVAL_LAUNCHING = 4f;
    static public final double INTERVAL_TRIGGER   = 1f;
    static public final double INTERVAL_DRIVE4    = 1f;
    static public final double INTERVAL_STOP      = 1f;
    static public final double INTERVAL_SPACING   = 1f;
    static public final double INTERVAL_DRIVE3    = 1f;
    static public final double INTERVAL_BUTTON1   = 1f;
    static public final double INTERVAL_BUTTON2   = 1f;
    static public final int    CLOSE_ENOUGH       = 6;
    static public final double INTERVAL_ROTATE    = 1f;

    @Override
    public void init()
    {
        // grab references to all of the software-hardware proxy objects

        motorLeftFrontWheels      = hardwareMap.get(DcMotor.class,          "lfwheel");
        motorLeftRearWheels       = hardwareMap.get(DcMotor.class,          "lrwheel");
        motorRightFrontWheels     = hardwareMap.get(DcMotor.class,          "rfwheel");
        motorRightRearWheels      = hardwareMap.get(DcMotor.class,          "rrwheel");
        motorAltitude             = hardwareMap.get(DcMotor.class,          "altitude");
        motorRotate               = hardwareMap.get(DcMotor.class,          "rotate");
        triggerServo              = hardwareMap.get(CRServo.class,          "trigger");
        clawServoLeft             = hardwareMap.get(Servo.class,            "clawleft");
        clawServoRight            = hardwareMap.get(Servo.class,            "clawright");
        motorLaunchLeft           = hardwareMap.get(DcMotor.class,          "launchleft");
        motorLaunchRight          = hardwareMap.get(DcMotor.class,          "launchright");
        colorSensor               = hardwareMap.get(ColorSensor.class,      "color");

        colorSensor.enableLed(false);

        // range sensor, taken from: http://www.modernroboticsinc.com/range-sensor

        rangeA = hardwareMap.i2cDevice.get("range28");
        rangeC = hardwareMap.i2cDevice.get("range2a");

        rangeAreader = new I2cDeviceSynchImpl(rangeA, I2cAddr.create8bit(0x28), false);
        rangeCreader = new I2cDeviceSynchImpl(rangeC, I2cAddr.create8bit(0x2a), false);

        rangeAreader.engage();
        rangeCreader.engage();

        motorAltitude.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorAltitude.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorAltitude.setPower(ENCODER_POWER);

        clawServoLeft.setPosition(STOW);
        clawServoRight.setPosition(STOW);
        triggerServo.setDirection(DcMotor.Direction.REVERSE);

        // configure the motors to default to the reverse of their typical direction,
        // to compensate for the motors needing to rotate in concert with their partner motors

        motorLaunchRight.setDirection(DcMotor.Direction.REVERSE);
        //motorRightWheels.setDirection(DcMotor.Direction.REVERSE);
        clawServoRight.setDirection(Servo.Direction.REVERSE);

        motorLaunchLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorLaunchRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // reset the timers & state variables before their first use

        barrelRaising      = false;
        spinUpTime         = 0f;
        buttonPressTime1   = 0f;
        buttonPressTime2   = 0f;
        triggerTime        = 0f;
        driveTime3         = 0f;
        buttonSensor1      = false;
        buttonSensor2      = false;
        spacingTime1       = 0f;
        startingAutonomous = true;
    }


    @Override
    public void loop()
    {
        // range sensor, taken from: http://www.modernroboticsinc.com/range-sensor

        rangeAcache = rangeAreader.read(0x04, 2);  //Read 2 bytes starting at 0x04
        rangeCcache = rangeCreader.read(0x04, 2);

        int RUS = rangeCcache[0] & 0xFF;   //Ultrasonic value is at index 0. & 0xFF creates a value between 0 and 255 instead of -127 to 128
        int LUS = rangeAcache[0] & 0xFF;
        int RODS = rangeCcache[1] & 0xFF;
        int LODS = rangeAcache[1] & 0xFF;

        // Spacing

        if (startingAutonomous)
        {
            startingAutonomous = false;
            leftSidewinderMovement(1f);
            spacingTime1 = time + INTERVAL_SPACING;
        }

        // Go to sensor wall

        if (spacingTime1 > 0f && spacingTime1 <= time)
        {
            spacingTime1 = 0f;
            straightLineMovement(-1f);
        }

        // http://www.modernroboticsinc.com/range-sensor

        if (LUS <= CLOSE_ENOUGH)
        {
            straightLineMovement(0f);
            stopTime1 = time + INTERVAL_STOP;
        }

        if (stopTime1 > 0f && stopTime1 <= time)
        {
            stopTime1     = 0f;
            leftSidewinderMovement(1f);
            buttonSensor1 = true;
        }

        if (buttonSensor1 && (colorSensor.red() >= RED_CUT_OFF ) && (colorSensor.blue() < BLUE))
        {
            buttonSensor1 = false;
            leftSidewinderMovement(0f);
            triggerServo.setPower(-1f);
            buttonPressTime1 = time + INTERVAL_BUTTON1;
        }

        if (buttonPressTime1 > 0f && buttonPressTime1 <= time)
        {
            buttonPressTime1 = 0f;
            triggerServo.setPower(1f);
            buttonRetractTime1 = time + INTERVAL_BUTTON1;
        }

        if (buttonRetractTime1 > 0f && buttonRetractTime1 <= time)
        {
            buttonRetractTime1 = 0f;
            triggerServo.setPower(0f);
            leftSidewinderMovement(1f);
            buttonSensor2 = true;
        }

        if (buttonSensor2 && (colorSensor.red() >= RED_CUT_OFF ) && (colorSensor.blue() < BLUE))
        {
            buttonSensor2 = false;
            leftSidewinderMovement(0f);
            triggerServo.setPower(-1f);
            buttonPressTime2 = time + INTERVAL_BUTTON1;
        }

        if (buttonPressTime2 > 0f && buttonPressTime2 <= time)
        {
            buttonPressTime2 = 0f;
            triggerServo.setPower(1f);
            buttonRetractTime2 = time + INTERVAL_BUTTON2;
        }

        if (buttonRetractTime2 > 0f && buttonRetractTime2 <= time)
        {
            buttonRetractTime2 = 0f;
            triggerServo.setPower(0f);
            straightLineMovement(.25f);
            driveTime3 = time + INTERVAL_DRIVE3;
        }

        if (driveTime3 > 0f && driveTime3 <= time)
        {
            driveTime3 = 0f;
            motorLeftFrontWheels.setPower(.25f);
            motorLeftRearWheels.setPower(.25f);
            motorRightFrontWheels.setPower(-.25f);
            motorRightRearWheels.setPower(-.25f);
            rotateTime3 = time + INTERVAL_ROTATE;
        }

        if (rotateTime3 > 0 && rotateTime3 <= time)
        {
            rotateTime3 = 0f;
            motorLeftFrontWheels.setPower(0f);
            motorLeftRearWheels.setPower(0f);
            motorRightFrontWheels.setPower(0f);
            motorRightRearWheels.setPower(0f);
            motorAltitude.setTargetPosition(ALTITUDE_FIRE);
            barrelRaising = true;
        }

        if (barrelRaising && !motorAltitude.isBusy())
        {
            barrelRaising = false;
            motorLaunchLeft.setPower(LAUNCH_POWER);
            motorLaunchRight.setPower(LAUNCH_POWER);
            spinUpTime = time + INTERVAL_LAUNCHING;
        }

        if (spinUpTime > 0f && spinUpTime <= time)
        {
            spinUpTime = 0f;
            triggerServo.setPower(TRIGGER_POWER);
            triggerTime = time + INTERVAL_TRIGGER;
        }

        if (triggerTime > 0f && triggerTime <= time)
        {
            triggerTime = 0f;
            motorLaunchLeft.setPower(0f);
            motorLaunchRight.setPower(0f);
            motorAltitude.setTargetPosition(0);
            straightLineMovement(.5f);
            driveTime4 = time + INTERVAL_DRIVE4;
        }

        if (driveTime4 > 0f && driveTime4 <= time)
        {
            driveTime4 = 0f;
            straightLineMovement(0f);
        }

        // print some helpful diagnostic messages to the driver controller app

        String currentState = "NONE";
        String spinUpState  = "OFF";

        if (startingAutonomous)
        {
            currentState = "startingAutonomous";
        }
        else if (spacingTime1 > 0f)
        {
            currentState = "spacingTime1";
        }
        else if (stopTime1 > 0f)
        {
            currentState = "stopTime1";
        }
        else if (buttonSensor1)
        {
            currentState = "buttonSensor1";
        }
        else if (buttonPressTime1 > 0f)
        {
            currentState = "buttonPressTime1";
        }
        else if (buttonRetractTime1 > 0f)
        {
            currentState = "buttonRetractTime1";
        }
        else if (buttonSensor2)
        {
            currentState = "buttonSensor2";
        }
        else if (buttonPressTime2 > 0f)
        {
            currentState = "buttonPressTime2";
        }
        else if (buttonRetractTime2 > 0f)
        {
            currentState = "buttonRetractTime2";
        }
        else if (rotateTime3 > 0f)
        {
            currentState = "rotateTime3";
        }
        else if (barrelRaising)
        {
            currentState = "barrelRaising";
        }
        else if (triggerTime > 0f)
        {
            currentState = "triggerTiome";
        }
        else if (driveTime3 > 0f)
        {
            currentState = "driveTime3";
        }
        else if (driveTime4 > 0f)
        {
            currentState = "driveTime4";
        }

        if (spinUpTime > 0f)
        {
            spinUpState = "ON";
        }

        // print some helpful diagnostic messages to the driver controller app

        telemetry.addData("state:", String.format("%s", currentState));
        telemetry.addData("sensors", String.format("color: R%d G%d B%d", colorSensor.red(), colorSensor.green(), colorSensor.blue()));
        telemetry.addData("arm", String.format("altitude: %d\ttarget: %d", motorAltitude.getCurrentPosition(), motorAltitude.getTargetPosition()));
        telemetry.addData("trigger", String.format("spin up: %s\ttrigger power: %.2f", spinUpState, triggerServo.getPower()));
        telemetry.addData("frontwheels", String.format("left: %.2f\tright: %.2f", motorLeftFrontWheels.getPower(), motorRightFrontWheels.getPower()));
        telemetry.addData("rearwheels", String.format("left: %.2f\tright: %.2f", motorLeftRearWheels.getPower(), motorRightRearWheels.getPower()));
        telemetry.addData("barrel:", String.format("left: %.2f\tright: %.2f", motorLaunchLeft.getPower(), motorLaunchRight.getPower()));

        // range sensor, taken from: http://www.modernroboticsinc.com/range-sensor

        //display values
        telemetry.addData("1 A US", LUS);
        telemetry.addData("2 A ODS", LODS);
        telemetry.addData("3 C US", RUS);
        telemetry.addData("4 C ODS", RODS);
    }

    public void straightLineMovement(double power)
    {
        motorRightFrontWheels.setPower(power);
        motorRightRearWheels.setPower(power);
        motorLeftFrontWheels.setPower(power);
        motorLeftRearWheels.setPower(power);
    }

    public void leftSidewinderMovement(double power)
    {
        motorLeftFrontWheels.setPower(-power);
        motorLeftRearWheels.setPower(power);
        motorRightFrontWheels.setPower(power);
        motorRightRearWheels.setPower(-power);
        //If the power is negative, the robot goes right. If the power is positive it goes left.
    }
}

