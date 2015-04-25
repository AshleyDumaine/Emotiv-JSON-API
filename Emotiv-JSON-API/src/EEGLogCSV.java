import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.StringJoiner;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
/** This class will be used for logging EEG sensor values into a text file for an R program to read **/
public class EEGLogCSV extends Frame implements KeyListener {

	private static final long serialVersionUID = 1L;
	public static boolean[] EEGEvents = new boolean[16];
	public EEGLogCSV() {
		addKeyListener(this); 
		setSize(320, 160);
		setVisible(true);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
	}
	public static void main(String[] args) {
		Pointer eEvent				= Edk.INSTANCE.EE_EmoEngineEventCreate();
		Pointer eState				= Edk.INSTANCE.EE_EmoStateCreate();
		IntByReference userID 		= null;
		IntByReference nSamplesTaken= null;
		int state  					= 0;
		float secs 					= 1;
		boolean readytocollect 		= false;
		userID 			= new IntByReference(0);
		nSamplesTaken	= new IntByReference(0);
		
		new EEGLogCSV();

		if (Edk.INSTANCE.EE_EngineConnect("Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
			System.out.println("Emotiv Engine start up failed.");
			return;
		}

		try {
			Scanner sc = new Scanner(System.in);
			System.out.println("Enter a file name for the csv");
			String fileName = sc.next();
			sc.close();
			String file = fileName + ".csv";
			PrintWriter writer = new PrintWriter(new FileOutputStream(file, false));
			System.out.println("Saving data to: " + file + "...");
			//close file on abrupt program termination
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					writer.close();
				}
			});

			Pointer hData = Edk.INSTANCE.EE_DataCreate();
			Edk.INSTANCE.EE_DataSetBufferSizeInSec(secs);
			System.out.print("Buffer size in secs: ");
			System.out.println(secs);

			System.out.println("Start receiving EEG Data!");
			while (true) {	
				state = Edk.INSTANCE.EE_EngineGetNextEvent(eEvent);

				// New event needs to be handled
				if (state == EdkErrorCode.EDK_OK.ToInt()) {
					int eventType = Edk.INSTANCE.EE_EmoEngineEventGetType(eEvent);
					Edk.INSTANCE.EE_EmoEngineEventGetUserId(eEvent, userID);

					// Log the EmoState if it has been updated
					if (eventType == Edk.EE_Event_t.EE_UserAdded.ToInt()) {
						if (userID != null)
						{
							System.out.println("User added");
							Edk.INSTANCE.EE_DataAcquisitionEnable(userID.getValue(),true);
							readytocollect = true;
						}
					}
				}
				else if (state != EdkErrorCode.EDK_NO_EVENT.ToInt()) {
					System.out.println("Internal error in Emotiv Engine!");
					break;
				}

				if (readytocollect) {
					Edk.INSTANCE.EE_DataUpdateHandle(0, hData);
					Edk.INSTANCE.EE_DataGetNumberOfSample(hData, nSamplesTaken);
					if (nSamplesTaken != null) {
						if (nSamplesTaken.getValue() != 0) {
							//System.out.print("Updated: "); //debug
							System.out.println(nSamplesTaken.getValue()); //debug
							double[] data = new double[nSamplesTaken.getValue()];
							for (int sampleIdx = 1; sampleIdx < nSamplesTaken.getValue(); sampleIdx++) { //start at 1
								StringJoiner joiner = new StringJoiner(",");
								for (int i = 1; i < 17; i++) { //3-17 are actual sensor values, 21 columns of data total
									Edk.INSTANCE.EE_DataGet(hData, i, data, nSamplesTaken.getValue());
									joiner.add(String.valueOf(data[sampleIdx] - data[sampleIdx - 1])); //use delta instead of raw value
								}
								//add 0 or 1 for blink or no blink based on key presses
								//joiner will make new columns for each EEG event
								for (int i = 0; i < 12; i++) {
									if (EEGEvents[i] == true) {
										joiner.add("1");
									}
									else {
										joiner.add("0");
									}
								}
								String joined = joiner.toString();
								writer.println(joined);
							}
						}
					}
				}
			}
			Edk.INSTANCE.EE_EngineDisconnect();
			Edk.INSTANCE.EE_EmoStateFree(eState);
			Edk.INSTANCE.EE_EmoEngineEventFree(eEvent);
			System.out.println("Disconnected!");
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
	}

	//EEG Events by index: Blink, LeftWink, RightWink, Furrow, RaiseBrow, Smile, Clench, 
	//LookLeft, LookRight, Laugh, SmirkLeft, SmirkRight, Cognitive_0, Cognitive_1, Cognitive_2, Cognitive_3
	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		switch (e.getKeyChar()) {
		case 'b':
			EEGEvents[0] = true; //blink
			System.out.println("blink activated");
			break;
		case 'l':
			EEGEvents[1] = true; //leftwink
			System.out.println("leftwink activated");
			break;
		case 'r':
			EEGEvents[2] = true; //rightwink
			System.out.println("rightwink activated");
			break;
		case 'f':
			EEGEvents[3] = true; //furrow
			System.out.println("furrow activated");
			break;
		case 'a':
			EEGEvents[4] = true; //raisebrow
			System.out.println("raisebrow activated");
			break;
		case 's':
			EEGEvents[5] = true; //smile
			System.out.println("smile activated");
			break;
		case 'c':
			EEGEvents[6] = true; //clench
			System.out.println("clench activated");
			break;
		case 'x':
			EEGEvents[7] = true; //lookleft
			System.out.println("lookleft activated");
			break;
		case 'y':
			EEGEvents[8] = true; //lookright
			System.out.println("lookright activated");
			break;
		case 'u':
			EEGEvents[9] = true; //laugh
			System.out.println("laugh activated");
			break;
		case 'i':
			EEGEvents[10] = true; //smirkleft
			System.out.println("smirkleft activated");
			break;
		case 'j':
			EEGEvents[11] = true; //smirkright
			System.out.println("smirkright activated");
			break;
		/*case '1':
			EEGEvents[12] = true; //cog 1
			System.out.println("cog 1 activated");
			break;
		case '2':
			EEGEvents[13] = true; //cog 2
			System.out.println("cog 2 activated");
			break;
		case '3':
			EEGEvents[14] = true; //cog 3
			System.out.println("cog 3 activated");
			break;
		case '4':
			EEGEvents[15] = true; //cog 4
			System.out.println("cog 4 activated");
			break;*/
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		switch (e.getKeyChar()) {
		//don't break, multiple keys can be pressed simultaneously
		case 'b':
			EEGEvents[0] = false; //blink
			System.out.println("blink deactivated");
			break;
		case 'l':
			EEGEvents[1] = false; //leftwink
			System.out.println("leftwink deactivated");
			break;
		case 'r':
			EEGEvents[2] = false; //rightwink
			System.out.println("rightwink deactivated");
			break;
		case 'f':
			EEGEvents[3] = false; //furrow
			System.out.println("furrow deactivated");
			break;
		case 'a':
			EEGEvents[4] = false; //raisebrow
			System.out.println("raisebrow deactivated");
			break;
		case 's':
			EEGEvents[5] = false; //smile
			System.out.println("smile deactivated");
			break;
		case 'c':
			EEGEvents[6] = false; //clench
			System.out.println("clench deactivated");
			break;
		case 'x':
			EEGEvents[7] = false; //lookleft
			System.out.println("lookleft deactivated");
			break;
		case 'y':
			EEGEvents[8] = false; //lookright
			System.out.println("lookright deactivated");
			break;
		case 'u':
			EEGEvents[9] = false; //laugh
			System.out.println("laugh deactivated");
			break;
		case 'i':
			EEGEvents[10] = false; //smirkleft
			System.out.println("smirkleft deactivated");
			break;
		case 'j':
			EEGEvents[11] = false; //smirkright
			System.out.println("smirkright deactivated");
			break;
		/*case '1':
			EEGEvents[12] = false; //cog 1
			System.out.println("cog 1 deactivated");
			break;
		case '2':
			EEGEvents[13] = false; //cog 2
			System.out.println("cog 2 deactivated");
			break;
		case '3':
			EEGEvents[14] = false; //cog 3
			System.out.println("cog 3 deactivated");
			break;
		case '4':
			EEGEvents[15] = false; //cog 4
			System.out.println("cog 4 deactivated");
			break;*/
		}
	}
}