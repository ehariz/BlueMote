package blueMotion;

public class CommandJNI {
	public native void muteSound();
	public native void pauseKey();
	public native void playKey();
	public native void nextKey();
	public native void previousKey();
	public native void increaseSound();
	public native void decreaseSound();
	static{
		System.loadLibrary("command64");
	}

}
