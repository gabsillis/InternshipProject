package application;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;


public class FXController
{
	@FXML
	private Button getNotesButton;
	@FXML
	private Button playNotesButton;
	// FXML camera button
	@FXML
	private Button cameraButton;
	// the FXML area for showing the current frame
	@FXML
	private ImageView originalFrame;
	// the FXML area for showing the mask
	@FXML
	private ImageView maskImage;
	// the FXML area for showing the output of the morphological operations
	@FXML
	private ImageView morphImage;
	// FXML slider for setting HSV ranges
	@FXML
	private Slider mHueStart;
	@FXML
	private Slider mHueStop;
	@FXML
	private Slider mSatStart;
	@FXML
	private Slider mSatStop;
	@FXML
	private Slider mValStart;
	@FXML
	private Slider mValStop;
	@FXML
	private Slider bHueStart;
	@FXML
	private Slider bHueStop;
	@FXML
	private Slider bSatStart;
	@FXML
	private Slider bSatStop;
	@FXML
	private Slider bValStart;
	@FXML
	private Slider bValStop;
	@FXML
	private Tab mCalTab;
	@FXML
	private Tab bCalTab;
	@FXML
	private ProgressBar progressBar;
	// FXML label to show the current values set with the sliders
	@FXML
	private Label hsvCurrentValues;
	
	playback player = new playback();
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that performs the video capture
	private ScheduledExecutorService getNotesTimer;
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive;
	
	// property for object binding
	private ObjectProperty<String> hsvValuesProp;
		
	@FXML
	private void playNotes(){
		player.play();
	}
	
	@FXML
	private void getNotes(){
		Mat frame = new Mat();
		int counter = 0;
		if (this.capture.isOpened()){
			try{
				this.capture.read(frame);
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		double[][] markerLocations = markerFinding(frame);
		double yMax = markerLocations[0][1];
		double yMin = markerLocations[markerLocations.length -1][1];
		double difference = yMax-yMin;
		if(difference < 0){
			double placeholder = yMax;
			yMax = yMin;
			yMin = placeholder;
		}
		double xbound = (markerLocations[0][0]+markerLocations[1][0])/2;
		double inchInPixels = Math.abs(difference/12);	
		
		boolean[][] notes = makeNoteArray(frame, yMin, xbound, inchInPixels);
		
		DataProcessing makefile = new DataProcessing();
		makefile.process(notes);
		
	}
	/**
	 * The action triggered by pushing the button on the GUI
	 */
	@FXML
	private void startCamera()
	{
		// bind a text property with the string containing the current range of
		// HSV values for object detection
		hsvValuesProp = new SimpleObjectProperty<>();
		this.hsvCurrentValues.textProperty().bind(hsvValuesProp);
				
		// set a fixed width for all the image to show and preserve image ratio
		this.imageViewProperties(this.originalFrame, 400);
		this.imageViewProperties(this.maskImage, 200);
		this.imageViewProperties(this.morphImage, 200);
		
		if (!this.cameraActive)
		{
			// start the video capture
			this.capture.open(0);
			
			// is the video stream available?
			if (this.capture.isOpened())
			{
				this.cameraActive = true;
				
				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						Image imageToShow = grabFrame();
						Platform.runLater(new Runnable(){
							@Override public void run(){originalFrame.setImage(imageToShow);}
						});
						
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				
				// update the button content
				this.cameraButton.setText("Stop Camera");
			}
			else
			{
				// log the error
				System.err.println("Failed to open the camera connection...");
			}
		}
		else
		{
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.cameraButton.setText("Start Camera");
			
			// stop the timer
			try
			{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log the exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
			
			// release the camera
			this.capture.release();
		}
	}
	
	
	private boolean[][] makeNoteArray(Mat frame, double yMin, double xbound, double inchInPixels){
		boolean[][] notes;
		Scalar[][] debugScalar = new Scalar[12][16];
		notes = new boolean[12][16];
		
		int xnumber = (int) Math.floor((frame.width()-xbound)/inchInPixels);
		if(xnumber > 16){
			xnumber = 16;
		}
		if(!frame.empty()){
			
			
			for(int i = 0; i<12; i++){
				for(int j=0;j<xnumber;j++){
					int x = (int)xbound + (j*((int)inchInPixels));
					int y = (int)yMin + (i*((int)inchInPixels));
					int width = (int)inchInPixels;
					int height = (int)inchInPixels;
					Rect rectCrop = new Rect(x,y,width,height);
					Mat crop = new Mat(frame ,rectCrop);
					
					Mat blurred = new Mat();
					Mat hsv = new Mat();
					Mat mask = new Mat();
					Mat morph = new Mat();
					
					Imgproc.blur(crop, blurred, new Size(7,7));
					Imgproc.cvtColor(blurred, hsv, Imgproc.COLOR_BGR2HSV);
					
					Scalar minValues = new Scalar(this.bHueStart.getValue(), this.bSatStart.getValue(),
							this.bValStart.getValue());
					Scalar maxValues = new Scalar(this.bHueStop.getValue(), this.bSatStop.getValue(),
							this.bValStop.getValue());
					
					Core.inRange(hsv, minValues, maxValues, mask);
					
					Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
					Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));
					
					Imgproc.erode(mask, morph, erodeElement);
					Imgproc.erode(mask,morph,erodeElement);
					Imgproc.dilate(mask, morph, dilateElement);
					Imgproc.dilate(mask, morph, dilateElement);
					Scalar mean = Core.mean(morph);
					debugScalar[i][j] = mean;
					notes[i][j] = mean.val[0] > 150; 					
				}
			}
		}
		return notes;
	}
	
	
	private double[][] markerFinding(Mat frame){
		double[][] locations;
		locations = null;
				if(!frame.empty()){
					Mat blurred = new Mat();
					Mat hsv = new Mat();
					Mat mask = new Mat();
					Mat morph = new Mat();
					
					Imgproc.blur(frame, blurred, new Size(7,7));
					Imgproc.cvtColor(blurred, hsv, Imgproc.COLOR_BGR2HSV);
					
					Scalar minValues = new Scalar(this.mHueStart.getValue(), this.mSatStart.getValue(),
							this.mValStart.getValue());
					Scalar maxValues = new Scalar(this.mHueStop.getValue(), this.mSatStop.getValue(),
							this.mValStop.getValue());
					
					Core.inRange(hsv, minValues, maxValues, mask);
					
					Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
					Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));
					
					Imgproc.erode(mask, morph, erodeElement);
					Imgproc.erode(mask,morph,erodeElement);
					Imgproc.dilate(mask, morph, dilateElement);
					Imgproc.dilate(mask, morph, dilateElement);
					
					List<MatOfPoint> contours = new ArrayList<>();
					Mat hierarchy = new Mat();
					
					// find contours
					Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
					
					List<Point> centers = new ArrayList<>();
					List<float[]> radiuses = new ArrayList<>();
					locations = new double[contours.size()][2];
					for(int i=0; i<contours.size();i++){
						MatOfPoint2f specificMat =  new MatOfPoint2f(contours.get(i).toArray());
						Point e = new Point();
						float[] e1 = null;
						centers.add(e);
						radiuses.add(e1);
						Imgproc.minEnclosingCircle(specificMat, centers.get(i), radiuses.get(i));
						locations[i][0] = e.x; locations[i][1] = e.y;
					}
					
				}
		return locations;
	}
	
	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private Image grabFrame()
	{
		// init everything
		Image imageToShow = null;
		Mat frame = new Mat();
		
		// check if the capture is open
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				this.capture.read(frame);
				
				// if the frame is not empty, process it
				if (!frame.empty())
				{
					// init
					Mat bBlurredImage = new Mat();
					Mat bHsvImage = new Mat();
					Mat bMask = new Mat();
					Mat bMorphOutput = new Mat();
					
					Mat mBlurredImage = new Mat();
					Mat mHsvImage = new Mat();
					Mat mMask = new Mat();
					Mat mMorphOutput = new Mat();
					
					// remove some noise
					Imgproc.blur(frame, bBlurredImage, new Size(7, 7));
					Imgproc.blur(frame, mBlurredImage, new Size(7,7));
					
					// convert the frame to HSV
					Imgproc.cvtColor(bBlurredImage, bHsvImage, Imgproc.COLOR_BGR2HSV);
					Imgproc.cvtColor(mBlurredImage, mHsvImage, Imgproc.COLOR_BGR2HSV);
					
					// get thresholding values from the UI
					// remember: H ranges 0-180, S and V range 0-255
					Scalar bMinValues = new Scalar(this.bHueStart.getValue(), this.bSatStart.getValue(),
							this.bValStart.getValue());
					Scalar bMaxValues = new Scalar(this.bHueStop.getValue(), this.bSatStop.getValue(),
							this.bValStop.getValue());
					Scalar mMinValues = new Scalar(this.mHueStart.getValue(), this.mSatStart.getValue(),
							this.mValStart.getValue());
					Scalar mMaxValues = new Scalar(this.mHueStop.getValue(), this.mSatStop.getValue(),
							this.mValStop.getValue());
					
					
					// threshold HSV image to select tennis balls
					Core.inRange(bHsvImage, bMinValues, bMaxValues, bMask);
					Core.inRange(mHsvImage, mMinValues, mMaxValues, mMask);
					// show the partial output
					
					
					
					// morphological operators
					// dilate with large element, erode with small ones
					Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
					Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));
					
					Imgproc.erode(bMask, bMorphOutput, erodeElement);
					Imgproc.erode(bMask, bMorphOutput, erodeElement);
					Imgproc.erode(mMask, mMorphOutput, erodeElement);
					Imgproc.erode(mMask,mMorphOutput, erodeElement);
					
					Imgproc.dilate(bMask, bMorphOutput, dilateElement);
					Imgproc.dilate(bMask, bMorphOutput, dilateElement);
					Imgproc.dilate(mMask, mMorphOutput, dilateElement);
					Imgproc.dilate(mMask, mMorphOutput, dilateElement);
					
					// show the partial output
					
					
					if(mCalTab.isSelected()){
						this.onFXThread(this.morphImage.imageProperty(), this.mat2Image(mMorphOutput));
						this.onFXThread(this.maskImage.imageProperty(), this.mat2Image(mMask));
						// find the tennis ball(s) contours and show them
						frame = this.findAndDrawBalls(mMorphOutput, frame);
						
						
						// convert the Mat object (OpenCV) to Image (JavaFX)
						imageToShow = mat2Image(frame);
						
						// show the current selected HSV range
						String valuesToPrint = "Hue range: " + mMinValues.val[0] + "-" + mMaxValues.val[0]
								+ "\tSaturation range: " + mMinValues.val[1] + "-" + mMaxValues.val[1] + "\tValue range: "
								+ mMinValues.val[2] + "-" + mMaxValues.val[2];
						this.onFXThread(this.hsvValuesProp, valuesToPrint);
					} else {
						this.onFXThread(this.morphImage.imageProperty(), this.mat2Image(bMorphOutput));
						this.onFXThread(this.maskImage.imageProperty(), this.mat2Image(bMask));
						// find the tennis ball(s) contours and show them
						frame = this.findAndDrawBalls(bMorphOutput, frame);
						
						
						// convert the Mat object (OpenCV) to Image (JavaFX)
						imageToShow = mat2Image(frame);
						
						// show the current selected HSV range
						String valuesToPrint = "Hue range: " + bMinValues.val[0] + "-" + bMaxValues.val[0]
								+ "\tSaturation range: " + bMinValues.val[1] + "-" + bMaxValues.val[1] + "\tValue range: "
								+ bMinValues.val[2] + "-" + bMaxValues.val[2];
						this.onFXThread(this.hsvValuesProp, valuesToPrint);
					}
					

					// print funciton for centers of mass
					printCenterOfMass(bMorphOutput, frame);
				}
				
			}
			catch (Exception e)
			{
				// log the (full) error
				System.err.print("ERROR");
				e.printStackTrace();
			}
		}
		
		return imageToShow;
	}
	
	/**
	 * Given a binary image containing one or more closed surfaces, use it as a
	 * mask to find and highlight the objects contours
	 * 
	 * @param maskedImage
	 *            the binary image to be used as a mask
	 * @param frame
	 *            the original {@link Mat} image to be used for drawing the
	 *            objects contours
	 * @return the {@link Mat} image with the objects contours framed
	 */
	private Mat findAndDrawBalls(Mat maskedImage, Mat frame)
	{
		// init
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		
		// find contours
		Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
		
		// if any contour exist...
		if (hierarchy.size().height > 0 && hierarchy.size().width > 0)
		{
			// for each contour, display it in blue
			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0])
			{
				Imgproc.drawContours(frame, contours, idx, new Scalar(250, 0, 0));
			}
		}
		
		return frame;
	}
	
	private void printCenterOfMass(Mat maskedImage, Mat frame){
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		
		// find contours
		Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
		
		List<Point> centers = new ArrayList<>();
		List<float[]> radiuses = new ArrayList<>();
		
		for(int i=0; i<contours.size();i++){
			MatOfPoint2f specificMat =  new MatOfPoint2f(contours.get(i).toArray());
			Point e = new Point();
			float[] e1 = null;
			centers.add(e);
			radiuses.add(e1);
			Imgproc.minEnclosingCircle(specificMat, centers.get(i), radiuses.get(i));
			System.out.println("Object number: "+i + " | Center x: " + centers.get(i).x + " | y: " + centers.get(i).y);
		}
	}
	/**
	 * Set typical {@link ImageView} properties: a fixed width and the
	 * information to preserve the original image ration
	 * 
	 * @param image
	 *            the {@link ImageView} to use
	 * @param dimension
	 *            the width of the image to set
	 */
	private void imageViewProperties(ImageView image, int dimension)
	{
		// set a fixed width for the given ImageView
		image.setFitWidth(dimension);
		// preserve the image ratio
		image.setPreserveRatio(true);
	}
	
	/**
	 * Convert a {@link Mat} object (OpenCV) in the corresponding {@link Image}
	 * for JavaFX
	 * 
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	private Image mat2Image(Mat frame)
	{
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer, according to the PNG format
		Imgcodecs.imencode(".png", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}
	
	/**
	 * Generic method for putting element running on a non-JavaFX thread on the
	 * JavaFX thread, to properly update the UI
	 * 
	 * @param property
	 *            a {@link ObjectProperty}
	 * @param value
	 *            the value to set for the given {@link ObjectProperty}
	 */
	private <T> void onFXThread(final ObjectProperty<T> property, final T value)
	{
		Platform.runLater(new Runnable() {
			
			@Override
			public void run()
			{
				property.set(value);
			}
		});
	}
	
}
