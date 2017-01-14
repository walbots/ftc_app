package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
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

    Servo   clawServoLeft;
    Servo   clawServoRight;
    CRServo triggerServo;

    // timer & state variables

    boolean barrelLowering;
    boolean barrelRaising;
    double  launchPower    = 0.3f;
    double  loadTime;
    double  reverseTime;
    double  spinUpTime;

    // constants to tweak certain movements

    static public final double FILTER_ALTITUDE        = 0.25f;
    static public final double FILTER_ROTATE          = 0.25f;
    static public final double PICK_UP                = 0f;
    static public final double LOAD                   = 0.75f;
    static public final double STOW                   = 1f;
    static public final int    ALTITUDE_UP            = -700;
    static public final int    ALTITUDE_DOWN          = 1200;
    static public final double ENCODER_POWER          = 0.75f;
    static public final double REVERSE_POWER          = -0.5f;
    static public final double CLAW_INCREMENT         = 0.1f;
    static public final double TRIGGER_POWER          = 0.5f;
    static public final double LAUNCH_POWER_INCREMENT = 0.0005f;

    // constants to use for timer intervals

    static public final double INTERVAL_LOAD      = 1f;
    static public final double INTERVAL_REVERSING = 1f;
    static public final double INTERVAL_TRIGGER   = 1f;

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
        motorAltitude.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        triggerServo.setPower(0f);

        // configure the motors to default to the reverse of their typical direction,
        // to compensate for the motors needing to rotate in concert with their partner motors

        triggerServo.setDirection(DcMotor.Direction.REVERSE);
        motorLaunchRight.setDirection(DcMotor.Direction.REVERSE);
        //motorRightWheels.setDirection(DcMotor.Direction.REVERSE);
        clawServoRight.setDirection(Servo.Direction.REVERSE);

        // reset the timers & state variables before their first use

        barrelLowering = false;
        loadTime       = 0f;
        reverseTime    = 0f;
        barrelRaising  = false;
        spinUpTime     = 0f;
    }

    @Override
    public void loop()
    {
        // compute the power from the appropriate gamepad input

        double powerLeftDrive  = determinePowerFromInput(gamepad1.left_stick_y);
        double powerRightDrive = determinePowerFromInput(gamepad1.right_stick_y);
        double powerRotate     = determinePowerFromInput(gamepad2. left_stick_x) * FILTER_ROTATE;
        double powerAltitude   = determinePowerFromInput(gamepad2.right_stick_y) * FILTER_ALTITUDE;

        // apply the computed power

        motorLeftWheels.setPower(powerLeftDrive);
        motorRightWheels.setPower(powerRightDrive);
        motorRotate.setPower(powerRotate);

        // clear any automatic elevation of the arm

        if (gamepad2.x)
        {
            motorAltitude.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        if (motorAltitude.getMode() != DcMotor.RunMode.RUN_TO_POSITION)
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
            // ignore trigger advance until the launch motors are spun up

            if (spinUpTime > 0f && spinUpTime <= time)
            {
                triggerServo.setPower(TRIGGER_POWER);
            }
        }
        else
        {
            triggerServo.setPower(0f);
        }

        // adjust launchPower

        if (gamepad2.y)
        {
            launchPower = launchPower + LAUNCH_POWER_INCREMENT;
            launchPower = Range.clip(launchPower, 0f, 1f);
        }
        else if (gamepad2.a)
        {
            launchPower = launchPower - LAUNCH_POWER_INCREMENT;
            launchPower = Range.clip(launchPower, 0f, 1f);
        }

        // trigger the launch motors

        if (gamepad2.right_bumper)
        {
            motorLaunchLeft.setPower(launchPower);
            motorLaunchRight.setPower(launchPower);

            // set a spinUpTime to freeze the trigger until the launch motors are ready

            if (spinUpTime == 0f)
            {
                spinUpTime = time + INTERVAL_TRIGGER;
            }
        }
        else // launch motors stop
        {
            spinUpTime = 0f;
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

        if (!motorAltitude.isBusy() && barrelLowering)
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
            clawServoLeft.setPosition(STOW);
            clawServoRight.setPosition(STOW);
            motorAltitude.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            triggerServo.setPower(0f);
        }

        // manually raise or lower the claw

        if (gamepad2.right_trigger > 0f)
        {
            clawServoRight.setPosition(Range.clip(clawServoRight.getPosition() + CLAW_INCREMENT, 0.0f, 1.0f));
            clawServoLeft.setPosition(Range.clip(clawServoLeft.getPosition() + CLAW_INCREMENT, 0.0f ,1.0f));
        }

        if (gamepad2.left_trigger > 0f)
        {
            clawServoRight.setPosition(Range.clip(clawServoRight.getPosition() - CLAW_INCREMENT, 0.0f, 1.0f));
            clawServoLeft.setPosition(Range.clip(clawServoLeft.getPosition() - CLAW_INCREMENT, 0.0f, 1.0f));
        }

        // print some helpful diagnostic messages to the driver controller app

        String currentState = "NONE";
        String spinUpState  = "OFF";

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

        if (spinUpTime > 0f)
        {
            spinUpState = "ON";
        }

        telemetry.addData("barrel:", String.format("left: %.2f\tright: %.2f", motorLaunchLeft.getPower(), motorLaunchRight.getPower()));
        telemetry.addData("trigger", String.format("spin up: %s\ttrigger power: %.2f", spinUpState, triggerServo.getPower()));
        telemetry.addData("arm", String.format("rotate: %.2f\taltitude: %.2f", powerRotate, powerAltitude));
        telemetry.addData("wheels", String.format("left: %.2f\tright: %.2f", powerLeftDrive, powerRightDrive));
        telemetry.addData("load:", String.format("alt: %d\ttgt: %d\tstate: %s", motorAltitude.getCurrentPosition(), motorAltitude.getTargetPosition(), currentState));
        telemetry.addData("launch power", String.format("currently: %.2f", launchPower));
    }

    @Override
    public void stop()
    {
        motorLeftWheels.resetDeviceConfigurationForOpMode();
        motorRightWheels.resetDeviceConfigurationForOpMode();
        motorAltitude.resetDeviceConfigurationForOpMode();
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
}
