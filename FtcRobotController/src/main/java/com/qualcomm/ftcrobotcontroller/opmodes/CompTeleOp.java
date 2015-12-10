/* Copyright (c) 2014 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.ftcrobotcontroller.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

/**
 * TeleOp Mode
 * <p>
 * Enables control of the robot via the gamepad
 */
public class CompTeleOp extends OpMode {

	final static double ARM_MIN_RANGE  = 0.20;
	final static double ARM_MAX_RANGE  = 0.90;
	final static double ARM_DELTA = 0.1;
	final static double ARM_FILTER = 0.1;

	/*
	 * Note: the configuration of the servos is such that
	 * as the arm servo approaches 0, the arm position moves up (away from the floor).
	 * Also, as the arm servo approaches 0, the arm opens up (drops the game element).
	 */
	// TETRIX VALUES.



	DcMotor motorRight;
	DcMotor motorBody;
	DcMotor motorLeft;
	DcMotor motorArm;
	Servo servoScoop;

	long time;

	/**
	 * Constructor
	 */
	public CompTeleOp() {
	}

	/*
	 * Code to run when the op mode is first enabled goes here
	 * 
	 * @see com.qualcomm.robotcore.eventloop.opmode.OpMode#start()
	 */
	@Override
	public void init() {
// this is a line of random text just for people who spend thier time reading scorce code comments
		//and the comment continues maby we will get a laugh when we go through this code later
		// but we probly wont look through it "i just found a new place to find a dead body"~ Luke
		//"90% of people dont look at scoce code" ya that does seem like something true

		/*
		 * Use the hardwareMap to get the dc motors and servos by name. Note
		 * that the names of the devices must match the names used when you
		 * configured your robot and created the configuration file.
		 */
		
		/*
		 * For the demo Tetrix K9 bot we assume the following,
		 *   There are two motors "motor_1" and "motor_2"
		 *   "motor_1" is on the right side of the bot.
		 *   "motor_2" is on the left side of the bot and reversed.
		 *   
		 * We also assume that there are two servos "servo_1" and "servo_6"
		 *    "servo_1" controls the arm joint of the manipulator.
		 *    "servo_6" controls the arm joint of the manipulator.
		 */
		motorRight = hardwareMap.dcMotor.get("motor_2");
		motorLeft = hardwareMap.dcMotor.get("motor_1");
		motorRight.setDirection(DcMotor.Direction.REVERSE);
		motorBody = hardwareMap.dcMotor.get("motor_3");
		motorArm = hardwareMap.dcMotor.get("motor_4");
		servoScoop = hardwareMap.servo.get("servo_1");
		servoScoop.scaleRange(ARM_MIN_RANGE, ARM_MAX_RANGE);
		servoScoop.setPosition(ARM_MIN_RANGE);
	}

	/*
	 * This method will be called repeatedly in a loop
	 * 
	 * @see com.qualcomm.robotcore.eventloop.opmode.OpMode#run()
	 */
	@Override
	public void loop() {

		/*
		 * Gamepad 1
		 * 
		 * Gamepad 1 controls the motors via the left and right sticks.
		 */
		double powerLeft = determinePowerFromInput(gamepad1.left_stick_y);
		double powerRight = determinePowerFromInput(gamepad1.right_stick_y);
		double powerRotate = determinePowerFromInput(gamepad2.right_stick_x);
		double powerArm = determinePowerFromInput(gamepad2.left_stick_y) * ARM_FILTER;
		boolean servoUp = gamepad2.right_bumper;
		boolean servoDown = gamepad2.left_bumper;
		double servoPosition = servoScoop.getPosition();

		motorRight.setPower(powerRight);
		motorLeft.setPower(powerLeft);
		motorBody.setPower(powerRotate);
		motorArm.setPower(powerArm);

		if (servoUp){
			servoPosition += ARM_DELTA;
		}
		else if (servoDown){
			servoPosition -= ARM_DELTA;
		}
		servoScoop.setPosition(servoPosition);

		/*
		 * Send telemetry data back to driver station. Note that if we are using
		 * a legacy NXT-compatible motor controller, then the getPower() method
		 * will return a null value. The legacy NXT-compatible motor controllers
		 * are currently write only.
		 */
        telemetry.addData("drive pwr", "lt : " + String.format("%.2f", powerLeft) + " rt: " + String.format("%.2f", powerRight));
		telemetry.addData("arm pwr", "ro: " + String.format("%.2f", powerRotate) + " el:" + String.format("%.2f", powerArm));
		telemetry.addData("scoop", "scoop " + (servoUp ? "UP" : (servoDown ? "DOWN" : "N/A")));
	}

	/*
	 * Code to run when the op mode is first disabled goes here
	 * 
	 * @see com.qualcomm.robotcore.eventloop.opmode.OpMode#stop()
	 */
	@Override
	public void stop() {

	}
        //Dead Body
	/* (
    	
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
