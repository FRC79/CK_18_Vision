import java.io.File;
import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
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
	
	public static void processImage(String srcpath, String dstpath) {
		// Load the native library.
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		// Load image
		Mat rawImage = Highgui.imread(srcpath);

		// Threshold the raw image into a binary image
		Mat binaryImage = thresholdHSV(rawImage);
		// Find contours and outline convex hull around shapes
		Mat convexHull = convexHull(binaryImage);
		

		// Write output
		Highgui.imwrite(dstpath, convexHull);
	}
	
	public static Mat thresholdHSV(Mat rawRGBImage){
		// Convert to HSV color space
		Mat hsv = new Mat();
		Imgproc.cvtColor(rawRGBImage, hsv, Imgproc.COLOR_BGR2HSV);

		// Split color channels
		ArrayList<Mat> dsts = new ArrayList<Mat>();
		Core.split(hsv, dsts);
		Mat hueSrc = dsts.get(0);
		Mat satSrc = dsts.get(1);
		Mat valSrc = dsts.get(2);

		// Threshold channels individually
		Mat hueDst = new Mat();
		Mat satDst = new Mat();
		Mat valDst = new Mat();
		Mat binTemp = new Mat();
		Mat binImg = new Mat();

		Imgproc.threshold(hueSrc, hueDst, HUE_MIN, HUE_MAX, Imgproc.THRESH_BINARY);
		Imgproc.threshold(satSrc, satDst, SAT_MIN, SAT_MAX, Imgproc.THRESH_BINARY);
		Imgproc.threshold(valSrc, valDst, VAL_MIN, VAL_MAX, Imgproc.THRESH_BINARY);
		
		// Combine operations to form binary image
		Core.bitwise_and(hueDst, satDst, binTemp);
		Core.bitwise_and(valDst, binTemp, binImg);
		
		// Return the thresholded binary image
		return binImg;
	}

	public static Mat convexHull(Mat binaryImage){
		// Find contours in the image
		ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(binaryImage, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
		
		// Find the convex hull object for each contour
		ArrayList<MatOfInt> hull = new ArrayList<MatOfInt>();
		for(int i=0; i < contours.size(); i++){
			hull.add(new MatOfInt());
		}
		for(int i=0; i < contours.size(); i++){
			Imgproc.convexHull(contours.get(i), hull.get(i));
		}
		
		// Convert MatOfInt to MatOfPoint to draw hull
		ArrayList<MatOfPoint> hullmop = new ArrayList<MatOfPoint>();
		for(int i=0; i < hull.size(); i++){
			MatOfPoint mopOut = new MatOfPoint();
			mopOut.create((int)hull.get(i).size().height,1,hull.get(i).type());
			
			for(int j=0; j < hull.get(i).size().height; j++){
				int index = (int)hull.get(i).get(j, 0)[0];
				double[] point = new double[] {
						contours.get(i).get(index, 0)[0], contours.get(i).get(index, 0)[1]
				};
				mopOut.put(j, 0, point);
				
				hullmop.add(mopOut);
			}
		}
		
		// Draw contours + hull results
		Mat overlay = new Mat(binaryImage.size(), CvType.CV_8UC3);
		Scalar color = new Scalar(0, 255, 0);	// Red
		for(int i=0; i < contours.size(); i++){
			Imgproc.drawContours(overlay, contours, i, color);
			Imgproc.drawContours(overlay, hullmop, i, color);
		}
		
		// Return contour drawn image
		return overlay;
	}
	
	public static void main(String[] args) {
		// Process all images in the testing folder
		File folder = new File("images/src");
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				processImage("images/src/" + listOfFiles[i].getName(),
						"images/dst/" + listOfFiles[i].getName());
			}
		}
	}
}
