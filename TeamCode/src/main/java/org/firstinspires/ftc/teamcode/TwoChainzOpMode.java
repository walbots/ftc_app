package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.Range;

/**
 * Created by Yanfeng on 9/28/16.
 */
@TeleOp(name="TwoChainz", group="Walbots")
public class TwoChainzOpMode extends OpMode
{
    DcMotor motorLeftWheels;
    DcMotor motorRightWheels;
    DcMotor motorRotate;
    DcMotor motorAltitude;
    DcMotor motorLaunchLeft;
    DcMotor motorLaunchRight;
    double launchTime = 0f;
    double coolTime = 0f;


    @Override
    public void init()
    {
        motorLeftWheels = hardwareMap.get(DcMotor.class,"leftwheel");
        motorRightWheels = hardwareMap.get(DcMotor.class,"rightwheel");
        motorAltitude = hardwareMap.get(DcMotor.class,"altitude");
        motorRotate = hardwareMap.get(DcMotor.class,"rotate");
        //motorLaunchLeft = hardwareMap.get(DcMotor.class,"launchleft");
        //motorLaunchRight = hardwareMap.get(DcMotor.class,"launchright");
        motorRightWheels.setDirection(DcMotor.Direction.REVERSE);
        //motorLaunchRight.setDirection(DcMotor.Direction.REVERSE);
    }

    @Override
    public void loop()
    {
        double powerLeftDrive = determinePowerFromInput(gamepad1.left_stick_y);
        double powerRightDrive = determinePowerFromInput(gamepad1.right_stick_y);
        double powerRotate = determinePowerFromInput(gamepad2. left_stick_x);
        double powerAltitude = determinePowerFromInput(gamepad2.right_stick_y);

        motorRightWheels.setPower(powerRightDrive);
        motorLeftWheels.setPower(powerLeftDrive);
        motorRotate.setPower(powerRotate);
        motorAltitude.setPower(powerAltitude);

        telemetry.addData("wheels", String.format("left: %.2f\tright: %.2f", powerLeftDrive, powerRightDrive));
        telemetry.addData("arm", String.format("rotate: %.2f\taltitude: %.2f", powerRotate, powerAltitude));

        if (gamepad2.right_bumper && coolTime <= time)
        {
            //motorLaunchLeft.setPower(1f);
            //motorLaunchRight.setPower(1f);
            launchTime = time + 1;
            coolTime = 0f;
        }

        if (launchTime <= time)
        {
            launchTime = 0f;
            //motorLaunchLeft.setPower(0f);
            //motorLaunchRight.setPower(0f);
            coolTime = time + 1;
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
