package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name="Snobot", group="Walbots")
public class SnoBotOpMode extends OpMode
{
    // software-hardware proxy object variables
    DcMotor motorBell;
    DcMotor motorLeftWheels;
    DcMotor motorRightWheels;
    // timer variables
    double bellTime;
    double wheelTime;
    double bellStopTime;
    // constants to use for intervals for timers
    static public final double BELL_POWER       = 1f;
    static public final double INTERVAL_RUNNING = 1f;
    static public final double INTERVAL_RINGING = 1f;
    static public final double BELL_STOP        = 0f;
    @Override
    public void init()
    {
        // grab references to all of the sofdtware-hardware proxy objects

        motorLeftWheels  = hardwareMap.get(DcMotor.class, "motor_1");
        motorRightWheels = hardwareMap.get(DcMotor.class, "motor_2");
        motorBell        = hardwareMap.get(DcMotor.class, "motor_bell");
        // configure the motors to default to the reverse of their typical direction,
        // to compensate for the motors needing to rotate in concert with their partner motors

        motorRightWheels.setDirection(DcMotor.Direction.REVERSE);

        // reset the timers before their first use
        bellTime    = 0f;
        wheelTime   = 0f;
        bellStopTime= 0f;
    }

    @Override
    public void loop()
    {
        if (gamepad1.dpad_up || gamepad1.dpad_down)
        {
            if (wheelTime == 0f)
            {
                wheelTime = time + INTERVAL_RUNNING; // set a wheelTime to stop the wheel motors after

                motorLeftWheels.setPower(0.5f);  // start the motor
                motorRightWheels.setPower(0.5f); // start the motor
            }
        }

        // if the wheelTime timer is set and has expired, stop the motors and clear the timer

        if (gamepad2.y)
        {
            motorBell.setPower(BELL_POWER);
            bellStopTime = time + INTERVAL_RINGING;
        }

        if (bellStopTime <= time && bellStopTime > 0f)
        {
            motorBell.setPower(BELL_STOP);
            bellStopTime = 0f;
        }

        if (wheelTime >= 0f && time >= wheelTime)
        {
            wheelTime = 0f; // reset the wheelTime for later use

            motorLeftWheels.setPower(0f);  // stop the motor
            motorRightWheels.setPower(0f); // stop the motor
        }

        // print some helpful diagnostic messages to the driver controller app

        telemetry.addData("y", String.format("%.2f", gamepad1.right_stick_y));
        telemetry.addData("x", String.format("%.2f", gamepad1.right_stick_x));

        // drive the wheel motors from a single stick

        motorLeftWheels.setPower(determineLeftWheelPowerFromSingleStickInput());
        motorRightWheels.setPower(determineRightWheelPowerFromSingleStickInput());
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
