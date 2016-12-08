package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;

@Autonomous(name="Snobot", group="Walbots")
public class SnoBotAutonomousOpMode extends OpMode
{
    // software-hardware proxy object variables

    DcMotor motorBell;
    DcMotor motorLeftWheels;
    DcMotor motorRightWheels;

    // timer variables

    double bellStopTime;
    double wheelTime;

    // constants to use for intervals for timers

    static public final double AUTO_POWER       = 0.5f;
    static public final double BELL_POWER       = 0.5f;
    static public final double DRIVE_FILTER     = 0.5f; // slow down the robot
    static public final double STOP             = 0f;
    static public final double INTERVAL_RUNNING = 1f;
    static public final double INTERVAL_RINGING = 0.5f;

    @Override
    public void init()
    {
        // grab references to all of the software-hardware proxy objects

        motorBell        = hardwareMap.get(DcMotor.class, "motor_bell");
        motorLeftWheels  = hardwareMap.get(DcMotor.class, "motor_1");
        motorRightWheels = hardwareMap.get(DcMotor.class, "motor_2");

        // configure the motors to default to the reverse of their typical direction,
        // to compensate for the motors needing to rotate in concert with their partner motors

        motorRightWheels.setDirection(DcMotor.Direction.REVERSE);

        // reset the timers before their first use

        bellStopTime = 0f;
        wheelTime    = 0f;
    }

    @Override
    public void loop()
    {
        if ()
        {
            motorLeftWheels.setPower(0f);
            motorRightWheels.setPower(0f);
        }
    }

    double determineLeftWheelPowerFromSingleStickInput()
    {
        double left_Power = 0f;
        double x_value    = gamepad1.right_stick_x;
        double y_value    = scaleInput(gamepad1.right_stick_y);

        if (x_value >= 0f)
        {
            left_Power = y_value;
        }

        telemetry.addData("left", String.format("%.2f", left_Power));

        return left_Power;
    }

    double determineRightWheelPowerFromSingleStickInput()
    {
        double right_Power = 0f;
        double x_value     = gamepad1.right_stick_x;
        double y_value     = scaleInput(gamepad1.right_stick_y);

        if (x_value <= 0f)
        {
            right_Power = y_value;
        }

        telemetry.addData("right", String.format("%.2f", right_Power));

        return right_Power;
    }

    double determinePowerFromInput(double dVal)
    {
        double power = dVal;

        // throttle: left_stick_y ranges from -1 to 1, where -1 is full up, and 1 is full down
        // direction: left_stick_x ranges from -1 to 1, where -1 is full left

        power = Range.clip(power, -1, 1);
        power = (float)scaleInput(power);

        power = power * DRIVE_FILTER;

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
