package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

@Autonomous(name="TwoChainzAuto", group="Walbots")
public class TwoChainzAutonomousOpMode extends OpMode
{
    // software-hardware proxy object variables

    DcMotor motorLeftWheels;
    DcMotor motorRightWheels;
    DcMotor motorAltitude;
    DcMotor motorRotate;
    DcMotor motorLaunchLeft;
    DcMotor motorLaunchRight;

    Servo   clawServoLeft;
    Servo   clawServoRight;
    CRServo triggerServo;

    // timer & state variables

    boolean startingAutonomous;
    double  driveTime1;
    boolean barrelRaising;
    double  launchTime;
    double  spinUpTime;
    boolean barrelLowering;
    double  driveTime2;

    // constants to tweak certain movements

    static public final double STOW             = 1f;
    static public final int    ALTITUDE_FIRE    = -1500;
    static public final int    ALTITUDE_UP      = 0;
    static public final double AUTO_DRIVE_POWER = -1f;
    static public final double ENCODER_POWER    = 0.75f;
    static public final double LAUNCH_POWER     = 0.3f;
    static public final double TRIGGER_POWER    = 0.5f;

    // constants to use for timer intervals

    static public final double INTERVAL_DRIVE1    = 1f;
    static public final double INTERVAL_LAUNCHING = 4f;
    static public final double INTERVAL_TRIGGER   = 1f;
    static public final double INTERVAL_DRIVE2    = 2f;

    @Override
    public void init()
    {
        // grab references to all of the software-hardware proxy objects

        motorLeftWheels  = hardwareMap.get(DcMotor.class, "leftwheel");
        motorRightWheels = hardwareMap.get(DcMotor.class, "rightwheel");
        motorAltitude    = hardwareMap.get(DcMotor.class, "altitude");
        motorRotate      = hardwareMap.get(DcMotor.class, "rotate");
        triggerServo     = hardwareMap.get(CRServo.class, "trigger");
        clawServoLeft    = hardwareMap.get(Servo.class,   "clawleft");
        clawServoRight   = hardwareMap.get(Servo.class,   "clawright");
        motorLaunchLeft  = hardwareMap.get(DcMotor.class, "launchleft");
        motorLaunchRight = hardwareMap.get(DcMotor.class, "launchright");

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

        // reset the timers & state variables before their first use

        driveTime1         = 0f;
        barrelRaising      = false;
        spinUpTime         = 0f;
        launchTime         = 0f;
        barrelLowering     = false;
        driveTime2         = 0f;
        startingAutonomous = true;
    }

    @Override
    public void loop()
    {
        // drive forward some

        if (startingAutonomous)
        {
            startingAutonomous = false;
            motorLeftWheels.setPower(AUTO_DRIVE_POWER);
            motorRightWheels.setPower(AUTO_DRIVE_POWER);
            driveTime1 = time + INTERVAL_DRIVE1;
        }

        // stop driving and raise the barrel for firing

        if (driveTime1 > 0f && driveTime1 <= time)
        {
            driveTime1 = 0f;
            motorRightWheels.setPower(0f);
            motorLeftWheels.setPower(0f);
            motorAltitude.setTargetPosition(ALTITUDE_FIRE);
            barrelRaising = true;
        }

        // spin up the motors

        if (barrelRaising && !motorAltitude.isBusy())
        {
            barrelRaising = false;
            motorLaunchLeft.setPower(LAUNCH_POWER);
            motorLaunchRight.setPower(LAUNCH_POWER);
            launchTime = time + INTERVAL_LAUNCHING;
            spinUpTime = time + INTERVAL_TRIGGER;
        }

        // fire

        if (spinUpTime > 0f && spinUpTime <= time)
        {
            spinUpTime = 0f;
            triggerServo.setPower(TRIGGER_POWER);
        }

        // stop the launch, reset the trigger and lower the barrel

        if (launchTime > 0f && launchTime <= time)
        {
            launchTime = 0f;
            motorLaunchLeft.setPower(0f);
            motorLaunchRight.setPower(0f);
            triggerServo.setPower(-TRIGGER_POWER);
            motorAltitude.setTargetPosition(ALTITUDE_UP);
            barrelLowering = true;
        }

        // drive forward to the center of the field

        if (barrelLowering && !motorAltitude.isBusy())
        {
            barrelLowering = false;
            motorLeftWheels.setPower(AUTO_DRIVE_POWER);
            motorRightWheels.setPower(AUTO_DRIVE_POWER);
            driveTime2 = time + INTERVAL_DRIVE2;
        }

        // stop driving and stop the trigger

        if (driveTime2 > 0f && driveTime2 <= time)
        {
            driveTime2 = 0f;
            motorLeftWheels.setPower(0f);
            motorRightWheels.setPower(0f);
            triggerServo.setPower(0f);
        }

        // print some helpful diagnostic messages to the driver controller app

        String currentState = "NONE";
        String spinUpState  = "OFF";

        if (startingAutonomous)
        {
            currentState = "startingAutonomous";
        }
        else if (driveTime1 > 0f)
        {
            currentState = "driveTime1";
        }
        else if (barrelRaising)
        {
            currentState = "barrelRaising";
        }
        else if (launchTime > 0f)
        {
            currentState = "launchTine";
        }
        else if (barrelLowering)
        {
            currentState = "barrelLowering";
        }
        else if (driveTime2 > 0f)
        {
            currentState = "driveTime2";
        }

        telemetry.addData("state:", String.format("%s", currentState));
        telemetry.addData("arm", String.format("altitude: %d\ttarget: %d", motorAltitude.getCurrentPosition(), motorAltitude.getTargetPosition()));
        telemetry.addData("trigger", String.format("spin up: %s\ttrigger power: %.2f", spinUpState, triggerServo.getPower()));
        telemetry.addData("wheels", String.format("left: %.2f\tright: %.2f", motorLeftWheels.getPower(), motorRightWheels.getPower()));
        telemetry.addData("barrel:", String.format("left: %.2f\tright: %.2f", motorLaunchLeft.getPower(), motorLaunchRight.getPower()));
    }
}

