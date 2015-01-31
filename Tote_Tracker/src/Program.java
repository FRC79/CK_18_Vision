import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;


public class Program {

	public static double processImage(String srcpath, String dstpath) {
		
		double startTime = System.currentTimeMillis();

		// Load image
		Mat rawImage = Highgui.imread(srcpath);
		
		// Threshold 
		// Convert to HSV color space
		Mat hsv = new Mat();
		Mat binImg = new Mat();
		Imgproc.cvtColor(rawImage, hsv, Imgproc.COLOR_BGR2HSV);
		Core.inRange(hsv, new Scalar(20,100,100), new Scalar(30, 255, 255), binImg);
		

		// Find contours
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Point center = new Point();
		int largestIndex = 0;
		double largestArea = 0;
		Rect largestBound = new Rect();
		
		Imgproc.findContours(binImg, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
		
		// Declare container for approximating polygons
		List<MatOfPoint> contours_poly = new ArrayList<MatOfPoint>(contours.size());
		for(MatOfPoint m : contours_poly){
			m = new MatOfPoint();
		}
		
		// Iterate through contours
		int minArea = 500;
		for(int i=0; i < contours.size(); i++){
			if(Imgproc.contourArea(contours.get(i)) > 1500){
				// Find contour with largest area
				// and save off the center values
				minArea = (int) Imgproc.contourArea(contours.get(i));
				
				Rect bound = Imgproc.boundingRect(contours.get(i));
				
//				if(bound.height*2 < bound.width){
//					continue;
//				}

				// Get biggest one
				if(Imgproc.contourArea(contours.get(i)) > largestArea){
					largestArea = Imgproc.contourArea(contours.get(i));
					largestIndex = i;		
					largestBound = bound;
					
					// Calculate the center of the contour using the nth order (1st order) moments
					// brush up on that calculus
//					Moments mu;
//					mu = Imgproc.moments(contours.get(i), false);
//					center = new Point(mu.get_m10()/mu.get_m00(), mu.get_m01()/mu.get_m00());
					center = new Point((bound.x + bound.width) - (bound.width/2.0),
							(bound.y + bound.height) - (bound.height/2.0));
				}
			}
		}
		
		//draw the final contour
//		Imgproc.drawContours(rawImage, contours, i, new Scalar(255,0,255), 3);
		Core.rectangle(rawImage, largestBound.tl(), largestBound.br(), new Scalar(255, 0, 255), 3);
		Core.circle(rawImage, center, 10, new Scalar(255, 0, 255), -10);
		
		// remap the center from top left to center of bottom
		center.x = (center.x - rawImage.width()/2.0);
		center.y = -(center.y - rawImage.height()/2.0);
		
		// Calculate angle to center of box
		double FOV_X = 58;
		double xRot = center.x/(rawImage.width()/2)*FOV_X/2;
		String text = "WIDTH: " + Integer.toString(largestBound.width) + 
				", HEIGHT: " + Integer.toString(largestBound.height);
//		String text = "X ROT: " + Double.toString(xRot);
		Core.putText(rawImage, text, new Point(20, rawImage.height()-40), Core.FONT_HERSHEY_COMPLEX_SMALL, 0.75, new Scalar(255,0,255));
		
		// Write output
		Highgui.imwrite(dstpath, rawImage);
		
		return (System.currentTimeMillis() - startTime) / 1000.0;
	}      
	
	public static void main(String[] args) {
		// Load the native library.
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		// Process all images in the testing folder
		File folder = new File("images/src");
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			System.out.println();
			System.out.println("FILE: " + listOfFiles[i].getName());
			System.out.println();
			if (listOfFiles[i].isFile()) {
				double dt = processImage("images/src/" + listOfFiles[i].getName(),
						"images/dst/" + listOfFiles[i].getName());
				
				System.out.println("Time Elapsed: " + dt);
				System.out.println("FPS: " + 1.0/dt);
				System.out.println();
			}
		}
	}

}
