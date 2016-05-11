// Copyright 2016, Waldorf School of the Peninsula.

package com.qualcomm.ftcrobotcontroller.opmodes;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

public class CompTeleOp extends OpMode
{
	final static double FILTER_ARM_DOWN = 0.2;
    final static double FILTER_ARM_UP = 0.6;
	final static double FILTER_DRIVE = 0.5;
    final static double FILTER_ROTATE = 0.4;
    final static double RANGE_ARM_MAX = 0.90;
    final static double RANGE_ARM_MIN = 0.20;
    final static double CLAW_START_POSITION = 0;


    DcMotor motorArm;         // this motor extends the arm
    DcMotor motorElbow;       // this motor drives the "elbow"
    DcMotor motorLeftWheels;  // these are the motors wired in series for the left wheels of the robot
	DcMotor motorRightWheels; // these are the motors wired in series for the right wheels of the robot
	DcMotor motorTorso;       // this motor drives the "torso" rotation
	Servo   servoClawLeft;    // this servo drives the left half of the claw
    Servo   servoClawRight;   // this servo drives the right half of the claw

	Context mediaPlayerContext;
	MediaPlayer mediaPlayer;

	public CompTeleOp()
    {
        // the constructor, not much to do here in this design
	}

    // Code to run when the op mode is first enabled goes here
    // @see com.qualcomm.robotcore.eventloop.opmode.OpMode#start()
	@Override
	public void init() {

		// Use the hardwareMap to get the dc motors and servos by name.
		// NOTE: The names of the devices must match the names used when you
		// configured your robot and created the configuration file in the
		// RobotController app, via ellipsis-vertical -> Settings -> Configure Robot.

		motorRightWheels = hardwareMap.dcMotor.get("motor_2");
		motorLeftWheels = hardwareMap.dcMotor.get("motor_1");
		motorRightWheels.setDirection(DcMotor.Direction.REVERSE);
		motorTorso = hardwareMap.dcMotor.get("motor_4");
        motorTorso.setDirection(DcMotor.Direction.REVERSE);
		motorElbow = hardwareMap.dcMotor.get("motor_3");
        motorElbow.setDirection(DcMotor.Direction.REVERSE);
        motorArm = hardwareMap.dcMotor.get("motor_5");
        servoClawLeft = hardwareMap.servo.get("servo_1");
		//servoClawLeft.scaleRange(RANGE_ARM_MIN, RANGE_ARM_MAX);
        servoClawRight = hardwareMap.servo.get("servo_2");
        //servoClawRight.scaleRange(RANGE_ARM_MIN, RANGE_ARM_MAX);

		servoClawLeft.setPosition(.5);
		servoClawRight.setPosition(.5);

		mediaPlayer = new MediaPlayer();
		mediaplayer.setDataSource(context, Uri.parse("android.resource://urpackagename/res/raw/theClaw");
	}

	/*
	 * This method will be called repeatedly in a loop
	 * 
	 * @see com.qualcomm.robotcore.eventloop.opmode.OpMode#run()
	 */
	@Override
	public void loop() {

		/*
		 * Gamepad 1 controls the motors via the left and right wheels.
		 *
		 * Gamepad2 controls the torso, elbow, and the right and left servos.
		 */
		double powerLeftDrive = determinePowerFromInput(gamepad1.left_stick_y) * FILTER_DRIVE;
		double powerRightDrive = determinePowerFromInput(gamepad1.right_stick_y) * FILTER_DRIVE;
		double powerRotateTorso = determinePowerFromInput(gamepad2.left_stick_x) * FILTER_ROTATE;
        double powerElbow = determinePowerFromInput(gamepad2.left_stick_y);
		double powerArmExtend = determinePowerFromInput(gamepad2.left_trigger);
        double powerArmRetract = determinePowerFromInput(gamepad2.right_trigger);
        double clawDirection = determinePowerFromInput(gamepad2.right_stick_y);

		if (gamepad2.right_stick_y != 0.0)
		{
			mediaPlayer.prepareAsync();
			mediaPlayer.pl
		}

        if (powerArmExtend > 0 && powerArmRetract > 0)
        {
            motorArm.setPower(0);
        }
        else if (powerArmExtend > 0)
        {
            motorArm.setDirection(DcMotor.Direction.FORWARD);
            motorArm.setPower(powerArmExtend);
        }
        else
        {
            motorArm.setDirection(DcMotor.Direction.REVERSE);
            motorArm.setPower(powerArmRetract);
        }

		clawDirection = Range.clip(clawDirection, .24, 1);

		double RightClawPosition = clawDirection;
		double LeftClawPostion = (1-clawDirection);

        motorElbow.setPower(powerElbow);
        motorRightWheels.setPower(powerRightDrive);
        motorLeftWheels.setPower(powerLeftDrive);
        motorTorso.setPower(powerRotateTorso);
		servoClawLeft.setPosition(LeftClawPostion);
		servoClawRight.setPosition(RightClawPosition);


		/*
		 * Send telemetry data back to driver station. Note that if we are using
		 * a legacy NXT-compatible motor controller, then the getPower() method
		 * will return a null value. The legacy NXT-compatible motor controllers
		 * are currently write only.
		 */
        telemetry.addData("drive pwr", "lft : " + String.format("%.2f", powerLeftDrive) + " rgt: " + String.format("%.2f", powerRightDrive));
       // telemetry.addData("arm pwr", "rot: " + String.format("%.2f", powerRotateTorso) + " elv:" + String.format("%.2f", powerArm));
        //telemetry.addData("scoop", "pos: " + String.format("%.2f", currentScoopPosition) + " " + (servoUp ? "UP" : (servoDown ? "DOWN" : "IDLE")));
	}

	/*
	 * Code to run when the op mode is first disabled goes here
	 * 
	 * @see com.qualcomm.robotcore.eventloop.opmode.OpMode#stop()
	 */
	@Override
	public void stop() {

	}
//fix me
	/*
	 * This method scales the joystick input so for low joystick values, the 
	 * scaled value is less than linear.  This is to make it easier to drive
	 * the robot more precisely at slower speeds.
	 */
	double scaleInput(double dVal)  {
		double[] scaleArray = { 0.0, 0.05, 0.09, 0.10, 0.12, 0.15, 0.18, 0.24,
				0.30, 0.36, 0.43, 0.50, 0.60, 0.72, 0.85, 1.00, 1.00 };
		
		// get the corresponding index for the scaleInput array.
		int index = (int) (dVal * 16.0);
		
		// index should be positive.
		if (index < 0) {
			index = -index;
		}

		// index cannot exceed size of array minus 1.
		if (index > 16) {
			index = 16;
		}

		// get value from the array.
		double dScale = 0.0;
		if (dVal < 0) {
			dScale = -scaleArray[index];
		} else {
			dScale = scaleArray[index];
		}

		// return scaled value.
		return dScale;
	}



	double determinePowerFromInput(double dVal)  {
		double power = dVal;

		// throttle: left_stick_y ranges from -1 to 1, where -1 is full up, and
		// 1 is full down
		// direction: left_stick_x ranges from -1 to 1, where -1 is full left

		power = Range.clip(power, -1, 1);
		power =  (float)scaleInput(power);

		return power;
	}
}
