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

package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.hardware.Servo;


/**
 * TeleOp Mode
 * <p>
 * Enables control of the robot via the gamepad
 */
public class Holladay_FareDriveTeleOp extends OpMode {

	/*
	 * Note: the configuration of the servos is such that
	 * as the arm servo approaches 0, the arm position moves up (away from the floor).
	 * Also, as the arm servo approaches 0, the arm opens up (drops the game element).
	 */
	// TETRIX VALUES.

	public enum ArmPosition { BACK, MIDDLE, FORWARD }

	final long intervalMilliseconds = 20;
	final float minimumArmSensitivity = Math.abs(0.5f);
	final double movementRate = 0.5;


	DcMotor motorRight;
	DcMotor motorLeft;
	//DcMotor motorArm;


	//ArmPosition currentArmPosition;
	//boolean directionIsForward;
	//long armMovementStopMilliseconds = 0;

	/**
	 * Constructor
	 */
	public Holladay_FareDriveTeleOp() {

	}

	/*
	 * Code to run when the op mode is first enabled goes here
	 * 
	 * @see com.qualcomm.robotcore.eventloop.opmode.OpMode#start()
	 */
	@Override
	public void init() {
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
		 * We also assume that there is one servo "servo_1"
		 *    "servo_1" controls the angle of the arm.
		 */
		motorRight = hardwareMap.dcMotor.get("motor_3");
		motorLeft = hardwareMap.dcMotor.get("motor_1");
		motorLeft.setDirection(DcMotor.Direction.REVERSE);

		//motorArm = hardwareMap.dcMotor.get("motor_2");

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
		 * Gamepad 1 controls the motors via the left stick, and it controls the
		 * wrist/arm via the a,b, x, y buttons
		 */

		// throttle: left_stick_y ranges from -1 to 1, where -1 is full up, and
		// 1 is full down
		// direction: left_stick_x ranges from -1 to 1, where -1 is full left
		// and 1 is full right
		float throttle = -gamepad1.left_stick_y;
		float direction = gamepad1.left_stick_x;
		float armthrottle = gamepad1.right_stick_y;

		float right = throttle - direction;
		float left = throttle + direction;

		// clip the right/left values so that the values never exceed +/- 1
		right = Range.clip(right, -1, 1);
		left = Range.clip(left, -1, 1);

		// scale the joystick value to make it easier to control
		// the robot more precisely at slower speeds.
		right = (float)scaleInput(right);
		left =  (float)scaleInput(left);
		
		// write the values to the motors
		motorRight.setPower(right);
		motorLeft.setPower(left);





		/*
		 * Send telemetry data back to driver station. Note that if we are using
		 * a legacy NXT-compatible motor controller, then the getPower() method
		 * will return a null value. The legacy NXT-compatible motor controllers
		 * are currently write only.
		 */
        telemetry.addData("Text", "*** Robot Data***");
        telemetry.addData("left tgt pwr",  "left  pwr: " + String.format("%.2f", left));
        telemetry.addData("right tgt pwr", "right pwr: " + String.format("%.2f", right));
//		telemetry.addData("arm position", "arm   pos: " + String.format("%.2d", armPosition));
	}

	/*
	 * Code to run when the op mode is first disabled goes here
	 * 
	 * @see com.qualcomm.robotcore.eventloop.opmode.OpMode#stop()
	 */
	@Override
	public void stop() {

	}

    	
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
		return dScale * 0.6;// the factor is to lower the power output to the motors
	}
/*
	ArmPosition moveArm (float throttle, ArmPosition armPosition)
	{
		ArmPosition result = armPosition;

		// if armMovementStopMilliseconds is not zero, move the arm in the specified direction
		// if armMovementStartMilliseoncds is zero, record current time there

		if (armMovementStopMilliseconds == 0)
		{
			if (Math.abs(throttle) >= minimumArmSensitivity)
			{
				boolean desiredDirectionIsForward = throttle > 0;

				if ((desiredDirectionIsForward && armPosition != ArmPosition.FORWARD) ||
						(!desiredDirectionIsForward && armPosition != ArmPosition.BACK))
				{
					armMovementStopMilliseconds = System.currentTimeMillis() + intervalMilliseconds;
					directionIsForward = desiredDirectionIsForward;
				}
			}
		}
		else // movement has been commanded to begin already
		{
			// check the time against the interval

			// if before end of interval, apply power
			if (System.currentTimeMillis() < armMovementStopMilliseconds)
			{
				double powerToApply = 0;

				if (directionIsForward)
				{
					powerToApply = movementRate;
				}
				else
				{
					powerToApply = movementRate * -1.0;
				}

				// continue moving the arm

				motorArm.setPower(powerToApply);
			}
			// if after end of interval, clear armMovementStopMilliseconds and change armPosition
			else
			{
				armMovementStopMilliseconds = 0;

				// we might have to actually tell the motor to stop
				motorArm.setPower(0.0);

				if (directionIsForward)
				{
					if (armPosition == ArmPosition.BACK)
					{
						result = ArmPosition.MIDDLE;
					}
					else // must have started in the middle
					{
						result = ArmPosition.FORWARD;
					}
				}
				else
				{
					if (armPosition == ArmPosition.FORWARD)
					{
						result = ArmPosition.MIDDLE;
					}
					else
					{
						result = ArmPosition.BACK;
					}
				}
			}
		}

		return result;
	}
*/
}
