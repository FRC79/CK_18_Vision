import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

public class Program {

	// Threshold Constants
	public static final double HUE_MIN = 60;
	public static final double HUE_MAX = 100;
	public static final double SAT_MIN = 90;
	public static final double SAT_MAX = 255;
	public static final double VAL_MIN = 20;
	public static final double VAL_MAX = 255;

	// Colors
	static final Scalar COLOR_GREEN = new Scalar(100, 255, 0);
	static final Scalar COLOR_BLUE = new Scalar(255, 191, 0);
	static final Scalar COLOR_YELLOW = new Scalar(0, 255, 255);
	static final Scalar COLOR_RED = new Scalar(0, 0, 255);
	
	public static double processImage(String srcpath, String dstpath) {
		
		double startTime = System.currentTimeMillis();

		// Load image
		Mat rawImage = Highgui.imread(srcpath);

		// Threshold the raw image into a binary image
		Mat binaryImage = new Mat();
		thresholdHSV(rawImage, binaryImage);
		
		// Find contours and outline convex hull around shapes
		List<SmartContour> contours = new ArrayList<SmartContour>();
		convexHull(binaryImage, contours);
		
		// Score the contours based on certain criteria
		scoreContours(rawImage, contours);

		// Write output
		Highgui.imwrite(dstpath, rawImage);
		
		return (System.currentTimeMillis() - startTime) / 1000.0;
	}

	public static void thresholdHSV(Mat rawRGBImage, Mat binaryDst) {
		// Convert to HSV color space
		Mat hsv = new Mat();
		Imgproc.cvtColor(rawRGBImage, hsv, Imgproc.COLOR_BGR2HSV);

		// Split color channels
		List<Mat> dsts = new ArrayList<Mat>();
		Core.split(hsv, dsts);
		Mat hueSrc = dsts.get(0);
		Mat satSrc = dsts.get(1);
		Mat valSrc = dsts.get(2);

		// Threshold channels individually
		Mat hueDst = new Mat();
		Mat satDst = new Mat();
		Mat valDst = new Mat();
		Mat binTemp = new Mat();

		Imgproc.threshold(hueSrc, hueDst, HUE_MIN, HUE_MAX,
				Imgproc.THRESH_BINARY);
		Imgproc.threshold(satSrc, satDst, SAT_MIN, SAT_MAX,
				Imgproc.THRESH_BINARY);
		Imgproc.threshold(valSrc, valDst, VAL_MIN, VAL_MAX,
				Imgproc.THRESH_BINARY);

		// Combine operations to form binary image (and return it)
		Core.bitwise_and(hueDst, satDst, binTemp);
		Core.bitwise_and(valDst, binTemp, binaryDst);
	}

	private static void mapHullPoints(List<MatOfPoint> contours,
			List<MatOfInt> hull, List<MatOfPoint> hullMOP) {

		// Convert MatOfInt of convex hull to MatOfPoint of contours

		// Loop over all contours
		List<Point[]> hullpoints = new ArrayList<Point[]>();
		for (int i = 0; i < hull.size(); i++) {
			Point[] points = new Point[hull.get(i).rows()];

			// Loop over all points that need to be hulled in current contour
			for (int j = 0; j < hull.get(i).rows(); j++) {
				int index = (int) hull.get(i).get(j, 0)[0];
				points[j] = new Point(contours.get(i).get(index, 0)[0],
						contours.get(i).get(index, 0)[1]);
			}

			hullpoints.add(points);
		}

		// Convert Point arrays into MatOfPoint
		for (int i = 0; i < hullpoints.size(); i++) {
			MatOfPoint mop = new MatOfPoint();
			mop.fromArray(hullpoints.get(i));
			hullMOP.add(mop);
		}
	}

	public static void convexHull(Mat binaryImage, List<SmartContour> contourDst) {
		// Find contours in the image
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(binaryImage, contours, new Mat(),
				Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

		// Find the convex hull object for each contour
		List<MatOfInt> hull = new ArrayList<MatOfInt>();
		for (int i = 0; i < contours.size(); i++) {
			hull.add(new MatOfInt());
		}
		for (int i = 0; i < contours.size(); i++) {
			Imgproc.convexHull(contours.get(i), hull.get(i));
		}

		// Map convex hull points to contour points (OpenCV Java fix)
		List<MatOfPoint> hullMOP = new ArrayList<MatOfPoint>();
		mapHullPoints(contours, hull, hullMOP);

		// Return contours into SmartContour List
		for(MatOfPoint mop : hullMOP){
			contourDst.add(new SmartContour(mop));
		}
	}

	public static void scoreContours(Mat rawImage, List<SmartContour> contours){

		// Iterate through contours and score to see if they meet criteria
		// BE CAREFULL WITH MIN AREA (different for 320x240)
		SmartContour cLeft = null, cRight = null;
		for(SmartContour c : contours){
			if(c.getAspectRatio() > 0.80 && c.getAspectRatio() < 2.0 && c.getArea() > 1200){
				if(c.isLeft()){
					if(cLeft == null){
						cLeft = c;
					}
				}
				if(c.isRight()){
					if(cRight == null){
						cRight = c;
					}
				}
			}
		}
		
		Core.line(rawImage, new Point(rawImage.width()/4, 0), 
				new Point(rawImage.width()/4, rawImage.height()), 
				COLOR_BLUE, 5);
		
		if(cLeft != null){
			Core.circle(rawImage, cLeft.getCenter(), 5, COLOR_GREEN, -5);
			Core.rectangle(rawImage, cLeft.getTopLeft(), cLeft.getBottomRight(), COLOR_GREEN, 3);
		}
		
		if(cRight != null){
			Core.rectangle(rawImage, cRight.getTopLeft(), cRight.getBottomRight(), COLOR_YELLOW, 3);
			Core.line(rawImage, new Point(rawImage.width()/4, cRight.getCenter().y), 
					cRight.getCenter(), COLOR_RED, 5);
			Core.circle(rawImage, cRight.getCenter(), 5, COLOR_YELLOW, -5);
		}
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
