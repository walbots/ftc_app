package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name="TwoChainz", group="Walbots")
public class TwoChainzOpMode extends OpMode
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
    // double    coolTime;
    double  launchTime;
    double  reverseTime;
    double  loadTime;
    double  grabTime;
    boolean barrelRaising;
    boolean barrelLowering;
    boolean barrelReadying;

    // constants to tweak certain movements

    static public final double FILTER_ALTITUDE    = 0.25f;
    static public final double FILTER_ROTATE      = 0.25f;
    static public final double TRIGGER_START      = 0.1f;
    static public final double TRIGGER_STOP       = 1f;
    static public final double PICK_UP              = 1f;
    static public final double LOAD                 = 0.3f;
    static public final double STOW                 = 0f;
    static public final int    ALTITUDE_UP          = -200;
    static public final int    ALTITUDE_DOWN        = 1200;
    static public final double ENCODER_POWER        = 0.75f;
    static public final double REVERSE_POWER        = -0.5f;

    // constants to use for timer intervals

    //static public double INTERVAL_COOLING   = 1f;
    static public final double INTERVAL_LAUNCHING   = 2f;
    static public final double INTERVAL_TRIGGER     = 0.5f;
    static public final double INTERVAL_REVERSING   = 1f;
    static public final double INTERVAL_PICKUP      = 1f;
    static public final double INTERVAL_LOAD        = 1f;

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
        motorAltitude.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // configure the motors to default to the reverse of their typical direction,
        // to compensate for the motors needing to rotate in concert with their partner motors

        motorLaunchRight.setDirection(DcMotor.Direction.REVERSE);
        //motorRightWheels.setDirection(DcMotor.Direction.REVERSE);
        clawServoRight.setDirection(Servo.Direction.REVERSE);
        // reset the timers & state variables before their first use
        triggerServo.setPosition(TRIGGER_START);
        //  coolTime    = 0f;
        launchTime      = 0f;
        servoWaitTime   = 0f;
        grabTime        = 0f;
        loadTime        = 0f;
        reverseTime     = 0f;
        barrelRaising   = false;
        barrelReadying  = false;
        barrelLowering  = false;
    }

    @Override
    public void loop()
    {
        String loadState = "NOT LOADING";

        // compute the power from the appropriate gamepad input

        double powerLeftDrive   = determinePowerFromInput(gamepad1.left_stick_y);
        double powerRightDrive  = determinePowerFromInput(gamepad1.right_stick_y);
        double powerRotate      = determinePowerFromInput(gamepad2. left_stick_x) * FILTER_ROTATE;
        double powerAltitude    = determinePowerFromInput(gamepad2.right_stick_y) * FILTER_ALTITUDE;

        // apply the computer power

        motorLeftWheels.setPower(powerLeftDrive);
        motorRightWheels.setPower(powerRightDrive);
        motorRotate.setPower(powerRotate);

        if (motorAltitude.getMode() != DcMotor.RunMode.RUN_TO_POSITION)
        {
            motorAltitude.setPower(powerAltitude);
        }

        // print some helpful diagnostic messages to the driver controller app

        telemetry.addData("wheels", String.format("left: %.2f\tright: %.2f", powerLeftDrive, powerRightDrive));
        telemetry.addData("arm", String.format("rotate: %.2f\taltitude: %.2f", powerRotate, powerAltitude));

        // manual override for the reverse process of the barrel loading

        if (gamepad2.dpad_down)
        {
            motorLaunchLeft.setPower(REVERSE_POWER);
            motorLaunchRight.setPower(REVERSE_POWER);
        }

        if (gamepad2.dpad_up)
        {
            motorLaunchLeft.setPower(0f);
            motorLaunchRight.setPower(0f);
        }

        // if the launch motors aren't cooling off,
        // trigger the launch motors when gamepad2.right_bumper is pressed

        if (gamepad2.right_bumper)// && coolTime <= time)
        {
            motorLaunchLeft.setPower(1f);           // fire launch motor at full power
            motorLaunchRight.setPower(1f);          // fire launch motor at full power
            launchTime = time + INTERVAL_LAUNCHING; // set a launchTime to stop the launch motors after
            //   coolTime = 0f;                          // reset the coolTime for later use
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
            // coolTime = time + INTERVAL_COOLING; // set coolTime to prevent the launch motors from burning out from repeated use
            triggerServo.setPosition(TRIGGER_START);
        }

        if (gamepad2.left_bumper)
        {
            motorAltitude.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            motorAltitude.setPower(ENCODER_POWER);
            motorAltitude.setTargetPosition (ALTITUDE_UP);
            barrelRaising = true;
        }

        if(!motorAltitude.isBusy() && barrelRaising)
        {
            barrelRaising = false;
            motorAltitude.setPower(0f);
            clawServoRight.setPosition(PICK_UP);
            clawServoLeft.setPosition(PICK_UP);
            grabTime = time + INTERVAL_PICKUP;
        }

        if (grabTime <= time && grabTime > 0f)
        {
            grabTime = 0f;
            motorAltitude.setPower(ENCODER_POWER);
            motorAltitude.setTargetPosition(ALTITUDE_DOWN);
            barrelLowering = true;
        }

        if(!motorAltitude.isBusy() && barrelLowering)
        {
            barrelLowering = false;
            motorAltitude.setPower(0f);
            clawServoRight.setPosition(LOAD);
            clawServoLeft.setPosition(LOAD);
            loadTime = time + INTERVAL_LOAD;
        }

        if (loadTime <= time && loadTime > 0f)
        {
            loadTime = 0f;
            motorLaunchLeft.setPower(REVERSE_POWER);
            motorLaunchRight.setPower(REVERSE_POWER);
            clawServoLeft.setPosition(PICK_UP);
            clawServoRight.setPosition(PICK_UP);
            triggerServo.setPosition(TRIGGER_START);
            reverseTime = time + INTERVAL_REVERSING;
        }

        if (reverseTime <= time && reverseTime > 0f)
        {
            reverseTime = 0f;
            motorLaunchLeft.setPower(0f);
            motorLaunchRight.setPower(0f);
            motorAltitude.setPower(ENCODER_POWER);
            motorAltitude.setTargetPosition (ALTITUDE_UP);
            barrelReadying = true;
        }

        if (barrelRaising)
        {
            loadState = "barrelRaising";
        }
        else if (grabTime > 0f)
        {
            loadState = "grabTime";
        }
        else if (barrelLowering)
        {
            loadState = "barrelLowering";
        }
        else if (loadTime > 0f)
        {
            loadState = "loadTime";
        }
        else if (reverseTime > 0f)
        {
            loadState = "reverseTime";
        }
        else if (barrelReadying)
        {
            loadState = "barrelReadying";
        }

        //telemetry.addData("barrel", String.format("launch: %.2f\tcool: %.2f", launchTime, coolTime));
        telemetry.addData("barrel:", String.format("launch: %.2f", launchTime));
        telemetry.addData("load:", String.format("alt: %d, tgt: %d, state: %s", motorAltitude.getCurrentPosition(), motorAltitude.getTargetPosition(), loadState));
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
}

