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
    // timer variables

   // double coolTime;
    double launchTime;
    // constants to tweak certain movements

    static public double FILTER_ALTITUDE    = 0.5f;
    static public double FILTER_ROTATE      = 0.5f;
    static public double TRIGGER_START      = 0.1f;
    static public double TRIGGER_STOP       = 1f;
    // constants to use for intervals for timers

    //static public double INTERVAL_COOLING   = 1f;
    static public double INTERVAL_LAUNCHING = 1f;

    @Override
    public void init()
    {
        // grab references to all of the software-hardware proxy objects

        motorLeftWheels  = hardwareMap.get(DcMotor.class, "leftwheel");
        motorRightWheels = hardwareMap.get(DcMotor.class, "rightwheel");
        motorAltitude    = hardwareMap.get(DcMotor.class, "altitude");
        motorRotate      = hardwareMap.get(DcMotor.class, "rotate");
        triggerServo     = hardwareMap.get(Servo.class,   "trigger");
        motorLaunchLeft  = hardwareMap.get(DcMotor.class, "launchleft");
        motorLaunchRight = hardwareMap.get(DcMotor.class, "launchright");

        // configure the motors to default to the reverse of their typical direction,
        // to compensate for the motors needing to rotate in concert with their partner motors

        motorLaunchRight.setDirection(DcMotor.Direction.REVERSE);
        motorRightWheels.setDirection(DcMotor.Direction.REVERSE);

        // reset the timers before their first use
        triggerServo.setPosition(TRIGGER_START);
      //  coolTime   = 0f;
        launchTime = 0f;
    }

    @Override
    public void loop()
    {
        // compute the power from the appropriate gamepad input

        double powerLeftDrive  = determinePowerFromInput(gamepad1.left_stick_y);
        double powerRightDrive = determinePowerFromInput(gamepad1.right_stick_y);
        double powerRotate     = determinePowerFromInput(gamepad2. left_stick_x) * FILTER_ROTATE;
        double powerAltitude   = determinePowerFromInput(gamepad2.right_stick_y) * FILTER_ALTITUDE;

        // apply the computer power

        motorLeftWheels.setPower(powerLeftDrive);
        motorRightWheels.setPower(powerRightDrive);
        motorRotate.setPower(powerRotate);
        motorAltitude.setPower(powerAltitude);

        // print some helpful diagnostic messages to the driver controller app

        telemetry.addData("wheels", String.format("left: %.2f\tright: %.2f", powerLeftDrive, powerRightDrive));
        telemetry.addData("arm", String.format("rotate: %.2f\taltitude: %.2f", powerRotate, powerAltitude));
        // if the launch motors aren't cooling off,
        // trigger the launch motors when gamepad2.right_bumper is pressed

        if (gamepad2.right_bumper)// && coolTime <= time)
        {
            //motorLaunchLeft.setPower(1f);           // fire launch motor at full power
            //motorLaunchRight.setPower(1f);          // fire launch motor at full power
            launchTime = time + INTERVAL_LAUNCHING; // set a launchTime to stop the launch motors after
         //   coolTime = 0f;                          // reset the coolTime for later use
            triggerServo.setPosition(TRIGGER_STOP);
        }

        if (launchTime <= time)
        {
            launchTime = 0f;                    // reset the launchTime for later use
            //motorLaunchLeft.setPower(0f);       // turn off the launch motor
            //motorLaunchRight.setPower(0f);      // turn off the launch motor
           // coolTime = time + INTERVAL_COOLING; // set coolTime to prevent the launch motors from burning out from repeated use
            triggerServo.setPosition(TRIGGER_START);
        }
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
