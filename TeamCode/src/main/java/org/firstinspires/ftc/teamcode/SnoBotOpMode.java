package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * Created by Yanfeng on 9/28/16.
 */
@TeleOp(name="Snobot", group="Walbots")
public class SnoBotOpMode extends OpMode
{
    DcMotor motorLeftWheels;
    DcMotor motorRightWheels;

    public void init()
    {
        motorLeftWheels = hardwareMap.get(DcMotor.class, "motor_1");
        motorRightWheels = hardwareMap.get(DcMotor.class, "motor_2");
        motorRightWheels.setDirection(DcMotor.Direction.REVERSE);

    }
    @Override

    public void loop()
    {
        motorLeftWheels.setPower(gamepad1.left_stick_y);
        motorRightWheels.setPower(gamepad1.right_stick_y);
    }


}


