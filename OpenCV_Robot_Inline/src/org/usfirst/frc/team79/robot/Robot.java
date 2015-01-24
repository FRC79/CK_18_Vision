package org.usfirst.frc.team79.robot;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {

	VideoCapture vcap;
	Mat rawImage;
	
	public void robotInit() {
		// Load the native library.
		System.load("/usr/local/lib/lib_OpenCV/java/libopencv_java2410.so");
		
		int videoStreamAddress = 0; // represents /dev/video0
		
		vcap = new VideoCapture();
		rawImage = new Mat();
		
		System.out.println("TRYING TO CONNECT");
		System.out.println();
		
		int count = 1;
		
		//open the video stream and make sure it's opened
		//We specify desired frame size and fps in constructor
		//Camera must be able to support specified framesize and frames per second
		//or this will set camera to defaults
		while(!vcap.open(videoStreamAddress, 320, 240, 7.5)){
			System.out.println("Error connecting to camera stream, retrying " + count);
			count++;
			Timer.delay(10);
		}
		
		//After Opening Camera we need to configure the returned image setting
		//all opencv v4l2 camera controls scale from 0.0 - 1.0


		System.out.println("DONE INITIALIZING");
		System.out.println();
	}

	public void autonomousPeriodic() {

	}

	public void teleopPeriodic() {
		// Read in new frame
		vcap.read(rawImage);
				
		System.out.println("STILL GOING...");
		
		Timer.delay(0.015);
	}

	public void testPeriodic() {

	}

}
