import java.io.File;
import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
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
		Mat image = Highgui.imread(srcpath);

		// Convert to HSV color space
		Mat hsv = new Mat();
		Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);

		// Split color channels
		ArrayList<Mat> dsts = new ArrayList<Mat>();
		Core.split(hsv, dsts);
		Mat hueSrc = dsts.get(0);
		Mat satSrc = dsts.get(1);
		Mat valSrc = dsts.get(2);

		// Threshold (Hue and Sat only)
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

		// Write output
		Highgui.imwrite(dstpath, binImg);
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
