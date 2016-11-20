package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
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

    Servo   triggerServo;
    Servo   clawServoLeft;
    Servo   clawServoRight;

    // timer & state variables

    double  servoWaitTime;
    double  launchTime;
    double  driveTime1;
    double  driveTime2;
    boolean barrelRaising;
    boolean barrelLowering;
    boolean startingAutonomous;

    // constants to tweak certain movements

    static public final double TRIGGER_START        = 0.1f;
    static public final double TRIGGER_STOP         = 1f;
    static public final double STOW                 = 0f;
    static public final int    ALTITUDE_FIRE        = -1500;
    static public final int    ALTITUDE_UP          = -200;
    static public final double AUTO_DRIVE_POWER     = -1f;

    // constants to use for timer intervals

    static public final double INTERVAL_LAUNCHING   = 2f;
    static public final double INTERVAL_TRIGGER     = 0.5f;
    static public final double INTERVAL_DRIVE1      = 1f;
    static public final double INTERVAL_DRIVE2      = 2f;

    @Override
    public void init()
    {
        // grab references to all of the software-hardware proxy objects

        motorLeftWheels  = hardwareMap.get(DcMotor.class, "leftwheel");
        motorRightWheels = hardwareMap.get(DcMotor.class, "rightwheel");
        motorAltitude    = hardwareMap.get(DcMotor.class, "altitude");
        motorRotate      = hardwareMap.get(DcMotor.class, "rotate");
        triggerServo     = hardwareMap.get(Servo.class,   "trigger");
        clawServoLeft    = hardwareMap.get(Servo.class,   "clawleft");
        clawServoRight   = hardwareMap.get(Servo.class,   "clawright");
        motorLaunchLeft  = hardwareMap.get(DcMotor.class, "launchleft");
        motorLaunchRight = hardwareMap.get(DcMotor.class, "launchright");

        motorAltitude.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorAltitude.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        // configure the motors to default to the reverse of their typical direction,
        // to compensate for the motors needing to rotate in concert with their partner motors

        motorLaunchRight.setDirection(DcMotor.Direction.REVERSE);
        //motorRightWheels.setDirection(DcMotor.Direction.REVERSE);
        clawServoRight.setDirection(Servo.Direction.REVERSE);

        // reset the timers & state variables before their first use

        clawServoLeft.setPosition(STOW);
        clawServoRight.setPosition(STOW);
        triggerServo.setPosition(TRIGGER_START);
        launchTime         = 0f;
        servoWaitTime      = 0f;
        driveTime1         = 0f;
        driveTime2         = 0f;
        barrelRaising      = false;
        barrelLowering     = false;
        startingAutonomous = true;
    }

    @Override
    public void loop()
    {
        String loadState = "NOT LOADING";

        if (barrelRaising)
        {
            loadState = "barrelRaising";
        }
        else if (barrelLowering)
        {
            loadState = "barrelLowering";
        }
        else if (startingAutonomous)
        {
            loadState = "startingAutonomous";
        }
        else if (launchTime > 0f)
        {
            loadState = "launchTine";
        }
        else if (driveTime1 > 0f)
        {
            loadState = "driveTime1";
        }
        else if (driveTime2 > 0f)
        {
            loadState = "driveTime2";
        }

        if (startingAutonomous) {
            startingAutonomous = false;
            motorLeftWheels.setPower(AUTO_DRIVE_POWER);
            motorRightWheels.setPower(AUTO_DRIVE_POWER);
            driveTime1 = time + INTERVAL_DRIVE1;
        }

        if (driveTime1 <= time && driveTime1 > 0f)
        {
            driveTime1 = 0f;
            motorRightWheels.setPower(0f);
            motorLeftWheels.setPower(0f);
            motorAltitude.setTargetPosition(ALTITUDE_FIRE);
            motorAltitude.setPower(0.25);
            barrelRaising = true;
        }

        if (!motorAltitude.isBusy() && barrelRaising)
        {
            barrelRaising = false;
            motorLaunchLeft.setPower(1f);           // fire launch motor at full power
            motorLaunchRight.setPower(1f);          // fire launch motor at full power
            launchTime = time + INTERVAL_LAUNCHING; // set a launchTime to stop the launch motors after
            servoWaitTime = time + INTERVAL_TRIGGER;
            //we gave the motors a chance to power up
        }

        // If servoWaitTime is enabled (>0) and servoWaitTime has expired, move the servo
        // to trigger the ball in the launcher

        if (servoWaitTime <= time && servoWaitTime > 0f)
        {
            triggerServo.setPosition(TRIGGER_STOP);
            servoWaitTime = 0f;
        }

        if (launchTime <= time && launchTime > 0f)
        {
            launchTime = 0f;                    // reset the launchTime for later use
            motorLaunchLeft.setPower(0f);       // turn off the launch motor
            motorLaunchRight.setPower(0f);      // turn off the launch motor
            triggerServo.setPosition(TRIGGER_START);
            motorAltitude.setTargetPosition(ALTITUDE_UP);
            barrelLowering = true;
        }

        if (!motorAltitude.isBusy() && barrelLowering)
        {
            barrelLowering = false;
            motorLeftWheels.setPower(AUTO_DRIVE_POWER);
            motorRightWheels.setPower(AUTO_DRIVE_POWER);
            driveTime2 = time + INTERVAL_DRIVE2;
        }

        if (driveTime2 <= time && driveTime2 > 0f)
        {
            driveTime2 = 0f;
            motorLeftWheels.setPower(0f);
            motorRightWheels.setPower(0f);
        }

        telemetry.addData("barrel:", String.format("launch: %.2f", launchTime));
        telemetry.addData("load:", String.format("alt: %d, tgt: %d, state: %s", motorAltitude.getCurrentPosition(), motorAltitude.getTargetPosition(), loadState));
    }
}

