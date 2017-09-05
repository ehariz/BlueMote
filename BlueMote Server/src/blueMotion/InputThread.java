package blueMotion;
import java.awt.Robot;
import java.awt.MouseInfo;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.Scanner;

import javax.microedition.io.StreamConnection;

public class InputThread implements Runnable {
	ByteArrayOutputStream result = new ByteArrayOutputStream();
	private StreamConnection comConnection;
	char[] charArray = new char[255];
	//int[] byteArray = new int[255];
	int bytes;
	int i = 0;
	String command ="";
	int byteCommand = 0;
	boolean key_switch = false;
	boolean commandProcessed = false;
	String prefix;
	Scanner sc = new Scanner(System.in);
	//Initializing to each command than can be received an integer
	
	public InputThread (StreamConnection connection)
	{
		comConnection = connection;
	}
	
	public void run() {
		try {
			//Preparing to receive commands 
			InputStream inputStream = comConnection.openInputStream();
			
			System.out.println("Waiting for input...");
			
			while (true) {
				bytes = inputStream.read();
				//System.out.println(bytes);
				if (bytes != 36) {
				//System.out.println(bytes);
				charArray[i] = (char) bytes;
				command+=charArray[i];
				//byteArray[i] = bytes;
				i++;
				}
				if (bytes == 36) {
					//command = String.valueOf(charArray);
					//byteCommand = String.valueOf(byteArray);
					System.out.println(command);
					//System.out.println(byteCommand);
					//System.out.println("processed");
					i = 0;
					charArray = new char[255];
					
					//byteArray = new int[255];
					processCommand(command);
					command="";
					commandProcessed = false;
				}
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/*
	private String streamToString(InputStream is) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		
		int length;
		while ((length = is.read(buffer)) != -1) {
		    result.write(buffer, 0, length);
		}
		return result.toString("UTF-8");
	}
	*/
	private void processCommand(String command) {
		try {
			prefix = stringExtract(command,0,'(');
			System.out.println(prefix);
			Robot robot = new Robot();
			System.out.println(command);
			if (prefix.equals("mouse_move")) {
				String xtmp = stringExtract (command,11,',');
				double xMove = Double.parseDouble(xtmp)/30;
				String ytmp = stringExtract (command,12+length(xtmp),')');
				double yMove = Double.parseDouble(ytmp)/30;
				double mouseX = MouseInfo.getPointerInfo().getLocation().getX();
				int moveToX = (int)(mouseX + xMove);
				double mouseY = MouseInfo.getPointerInfo().getLocation().getY();
				int moveToY = (int)(mouseY + yMove);
				System.out.println("moving mouse to ("+moveToX+","+moveToY+")");
				robot.mouseMove(moveToX, moveToY);
				commandProcessed = true;
				
			}

			if (commandProcessed == false) {
			switch (command) {
			case "key_switch" :
				if (key_switch == true) {
				key_switch = false;
				System.out.println("Command Enter");
				robot.keyPress(KeyEvent.VK_ENTER);
				robot.keyRelease(KeyEvent.VK_ENTER);
				}
				if (key_switch == false) {
				key_switch = true;
				System.out.println("Command Windows+Tab");
				robot.keyPress(KeyEvent.VK_WINDOWS);
				robot.keyPress(KeyEvent.VK_TAB);
				robot.keyRelease(KeyEvent.VK_WINDOWS);
				robot.keyRelease(KeyEvent.VK_TAB); 
				}
				
				
				break;
			
			case "key_left":
				System.out.println("Command Left");
				robot.keyPress(KeyEvent.VK_LEFT);
				robot.keyRelease(KeyEvent.VK_LEFT);
				break;
			
			case "key_right":
				System.out.println("Command Right");
				robot.keyPress(KeyEvent.VK_RIGHT);
				robot.keyRelease(KeyEvent.VK_RIGHT);

				commandProcessed = true;
				break;
				
			case "mouse_click":
				System.out.println("mouse click");
				robot.mousePress(InputEvent.BUTTON1_MASK);
			    robot.mouseRelease(InputEvent.BUTTON1_MASK);
			    break;
			    
			case "volumeUp":
				new CommandJNI().increaseSound();
				break;
				
			case "volumeDown":
				new CommandJNI().decreaseSound();
				break;
			
			case "mute":
				new CommandJNI().muteSound();
				break;
			
			case "next":
				new CommandJNI().nextKey();
				break;
				
			case "previous":
				new CommandJNI().previousKey();
				break;	
			
			case "playPause":
				new CommandJNI().playKey();
				break;	
				
				default:
					commandProcessed = true;
					System.out.println("Command unknown");}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		} 


	private String stringExtract(String word, int start, char delimiter) {
		String substring = "";
		char cursor = 'a';
		for (int i=start; i< length(word) && cursor != delimiter; i++ ) {
			cursor = word.charAt(i);
			if (cursor != delimiter) {
					substring += cursor;	
			}
		}
		return substring;	
	}

	private int length(String word) {
		return word.length();
	}
}


