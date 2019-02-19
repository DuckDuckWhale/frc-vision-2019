import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class Vision {
	public enum LogLevel {
		ERROR,
		WARNING,
		INFO,
		DEBUG
	}

	private static final String PREFIX[]={
		"Error",
		"Warning",
		"Info",
		"Debug"
	}, WINDOW_TITLE="Vision";
	private static final int CAMERA_WIDTH=640, CAMERA_HEIGHT=480, THRESHOLD=240;
	private static final double BLUR_RADIUS=0.9;

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static void log(LogLevel level, String message) {
		System.err.println("["+PREFIX[level.ordinal()]+"] "+message);
	}

	public static void error(String message) {log(LogLevel.ERROR, message);}
	public static void warning(String message) {log(LogLevel.WARNING, message);}
	public static void info(String message) {log(LogLevel.INFO, message);}
	public static void debug(String message) {log(LogLevel.DEBUG, message);}

	public static void printUsage() {
		error("Usage:\n"
				+"vision video <video> <fisheye (true/false)>\n"
				+"vision camera <index> <fisheye (true/false)>");
	}

	public static void main(String[] arg) {
		if (arg.length!=3) {
			printUsage();
			return;
		}

		Vision vision=new Vision();
		int result;
		boolean fisheye;

		switch (arg[2]) {
			case "true":
				fisheye=true;
				break;
			case "false":
				fisheye=false;
				break;
			default:
				error("The fisheye parameter is not a boolean.");
				System.exit(1);
				return;
		}
				
		switch (arg[0]) {
			case "video":
				result=vision.loop(arg[1], fisheye);
				break;
			case "camera":
				try {
					result=vision.loop(Integer.parseInt(arg[1]), fisheye);
				} catch (NumberFormatException e) {
					error("The index is not a valid integer.");
					result=1;
				}
				break;
			default:
				printUsage();
				result=1;
		}

		// forcefully exits the application
		// the window will not close if System.exit() is called
		System.exit(result);
	}

	private void adjustForFisheye(Mat frame) {
		Mat k=new Mat(3, 3, CvType.CV_64FC1), d=new Mat(2, 2, CvType.CV_64FC1), map_1=new Mat(), map_2=new Mat();

		k.put(0, 0, 272.2831082345223, 0.0, 330.5055386901159, 0.0, 272.06956412226043, 198.95483961693228, 0.0, 0.0, 1.0);
		d.put(0, 0, -0.03635131955587948, -0.02746289182992547, 0.03422666268794602, -0.0164632465730478);

		Calib3d.fisheye_initUndistortRectifyMap(k, d, Mat.eye(3, 3, CvType.CV_64FC1), k, new Size(CAMERA_WIDTH, CAMERA_HEIGHT), CvType.CV_16SC2,
				map_1, map_2);
		Imgproc.remap(frame, frame, map_1, map_2, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT);
	}

	private void process(Mat frame, List<MatOfPoint> contours, boolean fisheye) {
		double blurRadius = 0.9;
		int radius=(int)(blurRadius+0.5), kernel_size=2*radius+1;
		Size CAMERA_SIZE=new Size(CAMERA_WIDTH, CAMERA_HEIGHT), BLUR_SIZE=new Size(kernel_size, kernel_size);

		// resize
		if (!frame.size().equals(CAMERA_SIZE))
			Imgproc.resize(frame, frame, CAMERA_SIZE);
		// leave only the green channel (channel 1)
		Core.extractChannel(frame, frame, 1);
		// adjustments for the fisheye
		if (fisheye) adjustForFisheye(frame);
		// threshold
		Imgproc.threshold(frame, frame, THRESHOLD, 255, Imgproc.THRESH_BINARY);
		// blur
		Imgproc.blur(frame, frame, BLUR_SIZE);
		// enrode, iteration=1
		Imgproc.erode(frame, frame, new Mat(), new Point(-1, -1), 1, Core.BORDER_CONSTANT, new Scalar(-1));
		// find contours
		contours.clear();
		Imgproc.findContours(frame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
		// sort contours by area
		contours.sort(Comparator.comparingDouble((MatOfPoint contour) -> Imgproc.contourArea(contour, false)).reversed());
	}

	public int loop(int index, boolean fisheye) {
		VideoCapture camera=new VideoCapture(index);
		Mat frame=new Mat();
		ArrayList<MatOfPoint> contours=new ArrayList<MatOfPoint>();
		int total, frame_rate;
		long time=0, now;

		if (!camera.isOpened()) {
			error("Camera can't be opened!");
			return 1;
		}

		camera.set(Videoio.CAP_PROP_FRAME_WIDTH, CAMERA_WIDTH);
		camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, CAMERA_HEIGHT);
		frame_rate=(int)camera.get(Videoio.CAP_PROP_FPS);
		info("Frame rate: "+frame_rate);
		for (int count=2; camera.read(frame); ++count) {
			process(frame, contours, fisheye);
			HighGui.imshow(WINDOW_TITLE, frame);
			now=System.currentTimeMillis();
			HighGui.waitKey((int)Math.max(1, 1000/frame_rate-(now-time)));
			time=now;
			debug(Integer.toString(count));
		}

		frame.release();
		camera.release();
		HighGui.destroyAllWindows();

		return 0;
	}

	public int loop(String file, boolean fisheye) {
		VideoCapture camera=new VideoCapture(file);
		Mat frame=new Mat();
		ArrayList<MatOfPoint> contours=new ArrayList<MatOfPoint>();
		int total, frame_rate;
		long time=0, now;

		if (!camera.isOpened()) {
			error("Camera can't be opened!");
			return 1;
		}

		camera.set(Videoio.CAP_PROP_FRAME_WIDTH, CAMERA_WIDTH);
		camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, CAMERA_HEIGHT);
		frame_rate=(int)camera.get(Videoio.CAP_PROP_FPS);
		total=(int)camera.get(Videoio.CAP_PROP_FRAME_COUNT);
		info("Frame rate: "+frame_rate);
		info("Total frame count: "+total);

		camera.read(frame);
		process(frame, contours, fisheye);
		HighGui.imshow(WINDOW_TITLE, frame);
		HighGui.waitKey();

		for (int count=2; count<=total && camera.read(frame); ++count) {
			process(frame, contours, fisheye);
			HighGui.imshow(WINDOW_TITLE, frame);
			now=System.currentTimeMillis();
			HighGui.waitKey((int)Math.max(1, 1000/frame_rate-(now-time)));
			time=now;
			debug(Integer.toString(count));
		}

		frame.release();
		camera.release();
		HighGui.destroyAllWindows();

		return 0;
	}
}
