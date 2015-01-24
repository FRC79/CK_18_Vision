package org.usfirst.frc.team79.robot.camera;

import org.opencv.core.Mat;
import org.opencv.highgui.VideoCapture;


public class VisionService {
	
	private static final String NATIVE_LIBRARY_PATH = "/usr/local/lib/lib_OpenCV/java/libopencv_java2410.so";
	
	private static final int FRAME_WIDTH = 320;
	private static final int FRAME_HEIGHT = 240;
	private static final double FPS = 7.5;
	
	private static final double BUFFER_FLUSH_DELAY = 12; // seconds
	
	private static VisionService service;
	private static VideoCapture vcap;
	private static int videoStreamAddress = 0; // represents /dev/video0
	
	private static Object rawImgMutex = new Object();
	private static Object statusMutex = new Object();
	
	private static volatile Mat frame = new Mat();
	private static volatile boolean cameraConnected = false;
	private static volatile boolean processingImage = false;
	
	private VisionService(){
		// Load the native library.
		System.load(NATIVE_LIBRARY_PATH);
		
		// Start video capture thread
		Thread videoCaptureThread = new Thread(new VideoCaptureRunnable());
		videoCaptureThread.start();
	}
	
	public static VisionService getInstance(){
		return service;
	}
	
	public boolean cameraConnected(){
		synchronized(statusMutex){
			return cameraConnected;
		}
	}
	
	public boolean processingImage(){
		synchronized(statusMutex){
			return processingImage;
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
			synchronized (statusMutex) {
				cameraConnected = true;
			}
			
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
				synchronized(statusMutex){
					if((bufferDifference >= BUFFER_FLUSH_DELAY) && !processingImage){
						System.out.println("Buffer Cleared: Startin Processing Thread");
						processingImage = true;
					}
				}
				
				try {
					Thread.sleep(5); // Sleep for 5 millis to prevent infinite loop
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
