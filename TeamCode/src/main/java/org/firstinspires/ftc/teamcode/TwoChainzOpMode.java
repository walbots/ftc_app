package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDevice;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchImpl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;


@TeleOp(name="TwoChainz", group="Walbots")
public class TwoChainzOpMode extends OpMode
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

    // range sensor, taken from: http://www.modernroboticsinc.com/range-sensor

    byte[] rangeAcache;
    byte[] rangeCcache;

    I2cDevice rangeA;
    I2cDevice rangeC;
    I2cDeviceSynch rangeAreader;
    I2cDeviceSynch rangeCreader;

    // timer & state variables

    boolean barrelLowering;
    boolean barrelRaising;
    double  launchPower     = 0.015f;
    boolean launchPowerAdjusting;
    double  loadTime;
    double  reverseTime;

    // constants to tweak certain movements

    static public final double FILTER_ALTITUDE        = 0.25f;
    static public final double FILTER_ROTATE          = 0.25f;
    static public final double PICK_UP                = 0f;
    static public final double LOAD                   = 0.75f;
    static public final double STOW                   = 1f;
    static public final int    ALTITUDE_UP            = -1500;
    static public final int    ALTITUDE_DOWN          = 1400;
    static public final double ENCODER_POWER          = 0.75f;
    static public final double REVERSE_POWER          = -0.5f;
    static public final double TRIGGER_POWER          = 0.5f;
    static public final double LAUNCH_POWER_INCREMENT = 0.005f;

    // constants to use for timer intervals

    static public final double INTERVAL_LOAD      = 1f;
    static public final double INTERVAL_REVERSING = 2f;
    static public final double INTERVAL_TRIGGER   = 1f;

    @Override
    public void init()
    {
        // grab references to all of the software-hardware proxy objects

        motorLeftFrontWheels  = hardwareMap.get(DcMotor.class,     "lfwheel");
        motorRightFrontWheels = hardwareMap.get(DcMotor.class,     "rfwheel");
        motorLeftRearWheels   = hardwareMap.get(DcMotor.class,     "lrwheel");
        motorRightRearWheels  = hardwareMap.get(DcMotor.class,     "rrwheel");
        motorAltitude         = hardwareMap.get(DcMotor.class,     "altitude");
        motorRotate           = hardwareMap.get(DcMotor.class,     "rotate");
        triggerServo          = hardwareMap.get(CRServo.class,     "trigger");
        clawServoLeft         = hardwareMap.get(Servo.class,       "clawleft");
        clawServoRight        = hardwareMap.get(Servo.class,       "clawright");
        motorLaunchLeft       = hardwareMap.get(DcMotor.class,     "launchleft");
        motorLaunchRight      = hardwareMap.get(DcMotor.class,     "launchright");
        colorSensor           = hardwareMap.get(ColorSensor.class, "color");

        colorSensor.enableLed(false);

        // range sensor, taken from: http://www.modernroboticsinc.com/range-sensor

        rangeA = hardwareMap.i2cDevice.get("range28");
        rangeC = hardwareMap.i2cDevice.get("range2a");

        rangeAreader = new I2cDeviceSynchImpl(rangeA, I2cAddr.create8bit(0x28), false);
        rangeCreader = new I2cDeviceSynchImpl(rangeC, I2cAddr.create8bit(0x2a), false);

        rangeAreader.engage();
        rangeCreader.engage();

        // altitude motor setup

        motorAltitude.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorAltitude.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorAltitude.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // configure the motors to default to the reverse of their typical direction,
        // to compensate for the motors needing to rotate in concert with their partner motors

        triggerServo.setDirection(DcMotor.Direction.REVERSE);
        motorLaunchRight.setDirection(DcMotor.Direction.REVERSE);
        //motorRightWheels.setDirection(DcMotor.Direction.REVERSE);
        clawServoRight.setDirection(Servo.Direction.REVERSE);

        motorLaunchLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorLaunchRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // reset the timers & state variables before their first use

        barrelLowering       = false;
        loadTime             = 0f;
        reverseTime          = 0f;
        barrelRaising        = false;
        launchPowerAdjusting = false;
    }

    @Override
    public void loop()
    {
        // compute the power from the appropriate gamepad input

        double powerLeftDrive     = determinePowerFromInput(gamepad1.left_stick_y);
        double powerRightDrive    = determinePowerFromInput(gamepad1.right_stick_y);
        double powerRotate        = determinePowerFromInput(gamepad2.left_stick_x) * FILTER_ROTATE;
        double powerAltitude      = determinePowerFromInput(gamepad2.right_stick_y) * FILTER_ALTITUDE;
        double powerLeftMovement  = determinePowerFromInput(gamepad1.left_trigger);
        double powerRightMovement = determinePowerFromInput(gamepad1.right_trigger);
        double powerSidewinder    = (powerLeftMovement > 0f) ? -powerLeftMovement : powerRightMovement; // negative reverses the direction of the wheels as determined by the sidewinder method

        // apply the computed power

        motorRotate.setPower(powerRotate);

        if (powerSidewinder == 0f)
        {
            motorLeftFrontWheels.setPower(powerLeftDrive);
            motorLeftRearWheels.setPower(powerLeftDrive);
            motorRightFrontWheels.setPower(powerRightDrive);
            motorRightRearWheels.setPower(powerRightDrive);
        }
        else
        {
            leftSidewinderMovement(powerSidewinder, powerLeftDrive, powerRightDrive);
        }


        // zero the altitude and clear any automatic elevation of the arm

        if(gamepad2.x == true)
        {
            motorAltitude.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            motorAltitude.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motorAltitude.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
        else if (motorAltitude.getMode() != DcMotor.RunMode.RUN_TO_POSITION)
        {
            motorAltitude.setPower(powerAltitude);
        }

        // reverse or advance the trigger mechanism

        if (gamepad2.dpad_down)
        {
            triggerServo.setPower(-TRIGGER_POWER);
        }
        else if (gamepad2.dpad_up)
        {
            triggerServo.setPower(TRIGGER_POWER);
        }
        else
        {
            triggerServo.setPower(0f);
        }

        // adjust launchPower

        if (gamepad2.y && launchPowerAdjusting == false)
        {
            launchPower = launchPower + LAUNCH_POWER_INCREMENT;
            launchPower = Range.clip(launchPower, 0f, 1f);
            launchPowerAdjusting = true;
        }
        else if (gamepad2.a && launchPowerAdjusting == false)
        {
            launchPower = launchPower - LAUNCH_POWER_INCREMENT;
            launchPower = Range.clip(launchPower, 0f, 1f);
            launchPowerAdjusting = true;
        }
        else if (gamepad2.y == false && gamepad2.a == false)
        {
            launchPowerAdjusting = false;
        }

        // trigger the launch motors

        if (gamepad2.right_bumper)
        {
            motorLaunchLeft.setPower(launchPower);
            motorLaunchRight.setPower(launchPower);
        }
        else // launch motors stop
        {
            motorLaunchLeft.setPower(0f);
            motorLaunchRight.setPower(0f);
        }

        // automate the loading of the ball when the claw has grabbed it manually

        if (gamepad2.left_bumper)
        {
            triggerServo.setPower(Range.clip(-2f*TRIGGER_POWER, -1f, 0f));
            motorAltitude.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            motorAltitude.setPower(ENCODER_POWER);
            motorAltitude.setTargetPosition (ALTITUDE_DOWN);
            barrelLowering = true;
        }

        if (barrelLowering && !motorAltitude.isBusy())
        {
            barrelLowering = false;
            motorAltitude.setPower(0f);
            clawServoRight.setPosition(LOAD);
            clawServoLeft.setPosition(LOAD);
            loadTime = time + INTERVAL_LOAD;
        }

        if (loadTime > 0f && loadTime <= time)
        {
            loadTime = 0f;
            clawServoLeft.setPosition(PICK_UP);
            clawServoRight.setPosition(PICK_UP);
            motorLaunchLeft.setPower(REVERSE_POWER);
            motorLaunchRight.setPower(REVERSE_POWER);
            motorAltitude.setPower(ENCODER_POWER);
            motorAltitude.setTargetPosition (ALTITUDE_UP);
            reverseTime = time + INTERVAL_REVERSING;
        }

        if (reverseTime > 0f && reverseTime <= time)
        {
            reverseTime = 0f;
            motorLaunchLeft.setPower(0f);
            motorLaunchRight.setPower(0f);
            motorAltitude.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            triggerServo.setPower(0f);
        }

        // manually raise or lower the claw

        if (gamepad2.right_trigger > 0f)
        {
            clawServoRight.setPosition(STOW);
            clawServoLeft.setPosition(STOW);
        }
        else if (gamepad2.left_trigger > 0f)
        {
            clawServoRight.setPosition(PICK_UP);
            clawServoLeft.setPosition(PICK_UP);
        }

        // print some helpful diagnostic messages to the driver controller app

        String currentState = "NONE";

        if (barrelRaising)
        {
            currentState = "barrelRaising";
        }
        else if (barrelLowering)
        {
            currentState = "barrelLowering";
        }
        else if (loadTime > 0f)
        {
            currentState = "loadTime";
        }
        else if (reverseTime > 0f)
        {
            currentState = "reverseTime";
        }

        telemetry.addData("sensors", String.format("color: R%d G%d B%d", colorSensor.red(), colorSensor.green(), colorSensor.blue()));
        telemetry.addData("barrel:", String.format("left: %.2f\tright: %.2f", motorLaunchLeft.getPower(), motorLaunchRight.getPower()));
        telemetry.addData("trigger", String.format("trigger power: %.2f", triggerServo.getPower()));
        telemetry.addData("arm", String.format("rotate: %.2f\taltitude: %.2f", powerRotate, powerAltitude));
        telemetry.addData("wheels", String.format("left: %.2f\tright: %.2f", (powerLeftDrive == 0f) ? powerLeftMovement : powerLeftDrive, (powerRightDrive == 0f) ? powerRightMovement : powerRightDrive));
        telemetry.addData("load:", String.format("alt: %d\ttgt: %d\tstate: %s", motorAltitude.getCurrentPosition(), motorAltitude.getTargetPosition(), currentState));
        telemetry.addData("launch power", String.format("currently: %.3f", launchPower));
        telemetry.addData("claw", String.format("position: left %.2f\tright: %.2f", clawServoLeft.getPosition(), clawServoRight.getPosition()));

        // range sensor, taken from: http://www.modernroboticsinc.com/range-sensor

        rangeAcache = rangeAreader.read(0x04, 2);  //Read 2 bytes starting at 0x04
        rangeCcache = rangeCreader.read(0x04, 2);

        int RUS = rangeCcache[0] & 0xFF;   //Ultrasonic value is at index 0. & 0xFF creates a value between 0 and 255 instead of -127 to 128
        int LUS = rangeAcache[0] & 0xFF;
        int RODS = rangeCcache[1] & 0xFF;
        int LODS = rangeAcache[1] & 0xFF;

        //display values
        telemetry.addData("1 A US", LUS);
        telemetry.addData("2 A ODS", LODS);
        telemetry.addData("3 C US", RUS);
        telemetry.addData("4 C ODS", RODS);
    }

    @Override
    public void stop()
    {
        motorLeftFrontWheels.resetDeviceConfigurationForOpMode();
        motorRightFrontWheels.resetDeviceConfigurationForOpMode();
        motorLeftRearWheels.resetDeviceConfigurationForOpMode();
        motorRightRearWheels.resetDeviceConfigurationForOpMode();
        //motorAltitude.resetDeviceConfigurationForOpMode();
        motorRotate.resetDeviceConfigurationForOpMode();
        motorLaunchLeft.resetDeviceConfigurationForOpMode();
        motorLaunchRight.resetDeviceConfigurationForOpMode();
        clawServoLeft.resetDeviceConfigurationForOpMode();
        clawServoRight.resetDeviceConfigurationForOpMode();
        triggerServo.resetDeviceConfigurationForOpMode();
    }

    double determinePowerFromInput(double dVal)
    {
        double power = dVal;

        // throttle: left_stick_y ranges from -1 to 1, where -1 is full up, and 1 is full down
        // direction: left_stick_x ranges from -1 to 1, where -1 is full left

        power = Range.clip(power, -1, 1);
        power = (float)scaleInput(power);

        return power;
    }

    double scaleInput(double dVal)
    {
        double[] scaleArray = { 0.0, 0.05, 0.09, 0.10, 0.12, 0.15, 0.18, 0.24,
                0.30, 0.36, 0.43, 0.50, 0.60, 0.72, 0.85, 1.00, 1.00 };

        // get the corresponding index for the scaleInput array.
        int index = (int) (dVal * 16.0);

        // index should be positive.
        if (index < 0)
        {
            index = -index;
        }

        // index cannot exceed size of array minus 1.
        if (index > 16)
        {
            index = 16;
        }

        // get value from the array.
        double dScale = 0.0;

        if (dVal < 0)
        {
            dScale = -scaleArray[index];
        }
        else
        {
            dScale = scaleArray[index];
        }

        // return scaled value.
        return dScale;
    }

    public void leftSidewinderMovement(double power, double leftStick, double rightStick)
    {
        double leftFrontSidePower   = power;
        double rightFrontSidePower  = power;

        if (leftStick != 0f)
        {
            // bias rightFrontSidePower, so that we prefer leftFrontSidePower
            rightFrontSidePower = rightFrontSidePower * leftStick;
        }

        if (rightStick != 0f)
        {
            // bias rightFrontSidePower, so that we prefer leftFrontSidePower
            leftFrontSidePower = leftFrontSidePower * rightStick;
        }

        motorLeftFrontWheels.setPower(-leftFrontSidePower);
        motorLeftRearWheels.setPower(rightFrontSidePower);
        motorRightFrontWheels.setPower(-rightFrontSidePower);
        motorRightRearWheels.setPower(leftFrontSidePower);
    }
}
