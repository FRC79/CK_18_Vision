package org.usfirst.frc.team79.robot.camera;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import com.ni.vision.VisionException;


public class VisionService {
	
	public static final String NATIVE_LIBRARY_PATH = "/usr/local/lib/lib_OpenCV/java/libopencv_java2410.so";
	
	private static final int FRAME_WIDTH = 320;
	private static final int FRAME_HEIGHT = 240;
	private static final double FPS = 7.5;
	
	private static final int SERVER_PORT = 1180;
	
	private static final double BUFFER_FLUSH_DELAY = 12; // seconds
	
	private static VisionService service;
	private static VideoCapture vcap;
	private static int videoStreamAddress = 0; // represents /dev/video0
	
	private static volatile double toteX = 0, toteY = 0;
	
	private static Object rawImgMutex = new Object();
	private static Object measurementMutex = new Object();
	private static Object outputImgMutex = new Object();
	
	private static volatile Mat frame = new Mat();
	private static volatile Mat output = new Mat();
	private static volatile AtomicBoolean cameraConnected = new AtomicBoolean(false);
	private static volatile AtomicBoolean processingImage = new AtomicBoolean(false);
	private static volatile AtomicBoolean readyToServe = new AtomicBoolean(false);

	private static final int kHardwareCompression = -1;
	private static final byte[] kMagicNumber = { 0x01, 0x00, 0x00, 0x00 };
	private List<Byte> m_imageData;
	
	private VisionService(){
		// Start video capture thread
		m_imageData = new ArrayList<Byte>();
		
		Thread videoCaptureThread = new Thread(new VideoCaptureRunnable());
		Thread imgprocThread = new Thread(new ImageProcessingRunnable());
		Thread sendToDashboardThread = new Thread(new SendToDashboardRunnable());
		videoCaptureThread.start();
		imgprocThread.start();
		sendToDashboardThread.start();
	}
	
	public static VisionService getInstance(){
		if(service == null){
			service = new VisionService();
		}
		
		return service;
	}
	
	public boolean cameraConnected(){
		return cameraConnected.get();
	}
	
	public boolean processingImage(){
		return processingImage.get();
	}
	
	public double getToteX(){
		synchronized (measurementMutex) {
			return toteX;
		}
	}
	
	public double getToteY(){
		synchronized (measurementMutex) {
			return toteY;
		}
	}
	
	private class VideoCaptureRunnable implements Runnable {

		@Override
		public void run() {
			// Create timer variables
			long start, end, bufferStart, bufferEnd;
			start = System.currentTimeMillis();
			
			// Initialize VideoCapture object
			vcap = new VideoCapture();

			System.out.println();
			System.out.println("USB Webcam Server is trying to connect...");
			System.out.println();
			
			int count = 1;
			
			//open the video stream and make sure it's opened
			//We specify desired frame size and fps in constructor
			//Camera must be able to support specified framesize and frames per second
			//or this will set camera to defaults
			while(!vcap.open(videoStreamAddress, FRAME_WIDTH, FRAME_HEIGHT, FPS)){
				System.out.println("ERROR connecting to camera stream, retrying " + count);
				count++;
				try {
					Thread.sleep(1000); // Wait for 1 second to retry
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			//After Opening Camera we need to configure the returned image setting
			//all opencv v4l2 camera controls scale from 0.0 - 1.0


			System.out.println("Successfully connected to USB Camera!");
			System.out.println();
			
			// Set global boolean to true
			cameraConnected.set(true);
			
			// Calculate setup time for stream
			end = System.currentTimeMillis();
			System.out.println("It took " + ((start-end)/1000.0) + " seconds to set up stream");
			// Start timing for flushing the buffer
			bufferStart = System.currentTimeMillis();
			
			// Now, run this thread in a continuous loop
			while(true){
				synchronized (rawImgMutex) {
					vcap.read(frame);	// Load the current camera frame into a global variable
				}
				
				// End timer to get time since stream started
				bufferEnd = System.currentTimeMillis();
				double bufferDifference = (bufferEnd - bufferStart)/1000.0;
				
				//The stream takes a while to start up, and because of it, images from the camera
				//buffer. We don't have a way to jump to the end of the stream to get the latest image, so we
				//run this loop as fast as we can and throw away all the old images. This way, we wait some number of seconds
				//before we are at the end of the stream, and can allow processing to begin.
				if((bufferDifference >= BUFFER_FLUSH_DELAY) && !processingImage.get()){
					System.out.println("Buffer Cleared: Startin Processing Thread");
					processingImage.set(true);
				}
				
				try {
					Thread.sleep(5); // Sleep for 5 millis to prevent infinite loop
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private class ImageProcessingRunnable implements Runnable {

		@Override
		public void run() {
			// Create local processing variables
			Mat rawImage = new Mat();
			Mat hsv = new Mat();
			Mat binImage = new Mat();
			Mat outputImage = new Mat();
			boolean frameEmpty = true;
			
			// Continuously run loop
			while(true){
				// Check to see whether or not processing is enabled
				if(processingImage.get()){
					synchronized(rawImgMutex){
						if(!frame.empty()){
							// Copy global frame to local thread
							frame.copyTo(rawImage);
						}
						
						frameEmpty = frame.empty(); // Update local variable
					}
					
					// If there was a frame, process it
					if(!frameEmpty){
						processImage(rawImage, hsv, binImage, outputImage);
						
						synchronized (outputImgMutex) {
							outputImage.copyTo(output); // Copy the output image to the global variable
						}
						
						// Convert mat to JPEG that can be sent to Dashboard
						byte[] temp = new byte[output.rows() * output.cols() * output.channels()];
						output.get(0, 0, temp);
						ByteBuffer buffer = ByteBuffer.wrap(temp);
						m_imageData.clear();
						
						/* Find the start of the JPEG data */
						int index = 0;
						while (index < buffer.limit() - 1) {
							if ((buffer.get(index) & 0xff) == 0xFF
									&& (buffer.get(index + 1) & 0xff) == 0xD8)
								break;
							index++;
						}
						while (index < buffer.limit()) {
							m_imageData.add(buffer.get(index++));
						}

						if (m_imageData.size() <= 2) {
							throw new VisionException(
									"data size of flattened image is less than 2. Try another camera! ");
						}
						
						readyToServe.set(true);
					}
				}
				
				try {
					Thread.sleep(5); // Sleep for 5 millis to prevent infinite loop
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		private void processImage(Mat rawImage, Mat hsv, Mat binImage, Mat outputImage){
			// Threshold image with HSV tolerances for yellow
			Imgproc.cvtColor(rawImage, hsv, Imgproc.COLOR_BGR2HSV);
			Core.inRange(hsv, new Scalar(20,100,100), new Scalar(30, 255, 255), binImage);
			
			// Find contours
			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
			Point center = new Point();
			int largestIndex = 0;
			double largestArea = 0;
			Rect largestBound = new Rect();
			
			Imgproc.findContours(binImage, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
			
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
					
//					if(bound.height*2 < bound.width){
//						continue;
//					}

					// Get biggest one
					if(Imgproc.contourArea(contours.get(i)) > largestArea){
						largestArea = Imgproc.contourArea(contours.get(i));
						largestIndex = i;		
						largestBound = bound;
						
						// Calculate the center of the contour using the nth order (1st order) moments
						// brush up on that calculus
//						Moments mu;
//						mu = Imgproc.moments(contours.get(i), false);
//						center = new Point(mu.get_m10()/mu.get_m00(), mu.get_m01()/mu.get_m00());
						center = new Point((bound.x + bound.width) - (bound.width/2.0),
								(bound.y + bound.height) - (bound.height/2.0));
					}
				}
			}
			
			//draw the final contour
//			Imgproc.drawContours(rawImage, contours, i, new Scalar(255,0,255), 3);
			Core.rectangle(rawImage, largestBound.tl(), largestBound.br(), new Scalar(255, 0, 255), 3);
			Core.circle(rawImage, center, 10, new Scalar(255, 0, 255), -10);
			
			// remap the center from top left to center of bottom
			center.x = (center.x - rawImage.width()/2.0);
			center.y = -(center.y - rawImage.height()/2.0);
			
			// Calculate distance here
			synchronized (measurementMutex) {
				toteX = center.x;
				toteY = center.y;
			}
			
			// Output image (probably will need an "output frame" with a mutex
			// to allow for the server to catch it as well
			rawImage.copyTo(outputImage);
		}
		
	}
	
	private class SendToDashboardRunnable implements Runnable {

		@Override
		public void run() {
			try {
				serve();
			} catch (IOException e) {
				// do stuff here
			} catch (InterruptedException e) {
				// do stuff here
			}
		}
	}

	public void serve() throws IOException, InterruptedException {
		ServerSocket socket = new ServerSocket();
		socket.setReuseAddress(true);
		InetSocketAddress address = new InetSocketAddress(SERVER_PORT);
		socket.bind(address);

		while (true) {
			try {
				Socket s = socket.accept();

				DataInputStream is = new DataInputStream(s.getInputStream());
				OutputStream os = s.getOutputStream();

				int fps = is.readInt();
				int compression = is.readInt();
				int size = is.readInt();

				if (compression != kHardwareCompression ) {
					// print to driverstation -->
					// ("Choose \"USB Camera HW\" on the dashboard");
					s.close();
					continue;
				}

				long period = (long) (1000 / (1.0 * fps));
				while (true) {

					long t0 = System.currentTimeMillis();

					while (!readyToServe.get())
						;
					readyToServe.set(false);

					// write numbers
					try {

						os.write(kMagicNumber);

						// write size of image

						synchronized (this) {
							os.write(intTobyteArray(m_imageData.size()));

							byte[] imageData = new byte[m_imageData.size()];

							for (int i = 0; i < m_imageData.size(); i++) {
								imageData[i] = m_imageData.get(i).byteValue();
							}

							os.write(imageData);
						}
						long dt = System.currentTimeMillis() - t0;

						if (dt < period) {
							Thread.sleep(period - dt);
						}

					} catch (IOException ex) {
						// print error to driverstation
						break;
					}
				}

			} catch (IOException ex) {
				// print error to driverstation
				continue;
			}
		}
	}
	
	private byte[] intTobyteArray(int n) {
		byte[] arr = new byte[4];

		arr[0] = (byte) ((n >> 24) & 0xFF);
		arr[1] = (byte) ((n >> 16) & 0xFF);
		arr[2] = (byte) ((n >> 8) & 0xFF);
		arr[3] = (byte) (n & 0xFF);

		return arr;
	}
}
