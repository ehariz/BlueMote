package blueMotion;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.UUID;
import javax.bluetooth.LocalDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

public class ListenThread implements Runnable {
		public ListenThread() {
			
		}
		public void run() {
			LocalDevice local = null;
			
			StreamConnectionNotifier notif;
			StreamConnection connection = null; 
			
			try {
				local = LocalDevice.getLocalDevice();
				local.setDiscoverable(DiscoveryAgent.GIAC); //Retrieving local BT device
				UUID uuid = new UUID("0000110100001000800000805F9B34FB",false);
				String url = "btspp://localhost:"+uuid.toString()+";name=BlueMotion";
				notif = (StreamConnectionNotifier)Connector.open(url);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			while(true) {
				try {
					System.out.println("Waiting for connection");
					connection = notif.acceptAndOpen();
					Thread processThread = new Thread(new InputThread(connection)); 
					processThread.start();
					// When a connection is detected, launch an InputData to process incoming commands
				
				} catch (Exception e)
				{
					e.printStackTrace();
					return;
				}
			}
		}

}
