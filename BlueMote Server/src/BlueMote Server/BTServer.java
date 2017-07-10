package blueMotion;

public class BTServer {
		public static void main(String[] args) {
			Thread waitThread = new Thread(new ListenThread()); 
			waitThread.start(); // Creating and launching a ListenThread
		}
}
