import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
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

	private static String PREFIX[]={
		"Error",
		"Warning",
		"Info",
		"Debug"
	}, WINDOW_TITLE="Vision";
	private static int CAMERA_WIDTH=640, CAMERA_HEIGHT=480, THRESHOLD=240;
	private static Size SIZE=new Size(CAMERA_WIDTH, CAMERA_HEIGHT);

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

	public static void main(String[] arg) {
		// forcefully exits the application due to the window not closing
		if (arg.length!=1) {
			error("Usage: vision [video]");
			return;
		}
		System.exit(new Vision().loop(arg[0]));
	}

	public void resize(Mat frame) {
		if (!frame.size().equals(SIZE))
			Imgproc.resize(frame, frame, SIZE);
	}

	public void greenOnly(Mat frame) {
		// leave only the green channel
		// green channel is 1
		Core.extractChannel(frame, frame, 1);
	}

	public void threshold(Mat frame) {
		Imgproc.threshold(frame, frame, THRESHOLD, 255, Imgproc.THRESH_BINARY);
	}

	public void blur(Mat frame) {
		double blurRadius = 0.9;
		int radius=(int)(blurRadius+0.5), kernel_size=2*radius+1;

		Imgproc.blur(frame, frame, new Size(kernel_size, kernel_size));
	}

	public void process(Mat frame) {
		resize(frame);
		greenOnly(frame);
		threshold(frame);
		blur(frame);
	}

	public int loop(String file) {
		VideoCapture camera=new VideoCapture(file);
		Mat frame=new Mat();
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
		process(frame);
		HighGui.imshow(WINDOW_TITLE, frame);
		HighGui.waitKey();

		for (int count=2; count<=total && camera.read(frame); ++count) {
			process(frame);
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
