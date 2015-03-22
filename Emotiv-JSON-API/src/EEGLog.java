import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.StringJoiner;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
/** This class will be used for logging EEG sensor values and sending them over a socket to an R program **/
public class EEGLog {
	public static void main(String[] args) throws Exception {
		Pointer eEvent				= Edk.INSTANCE.EE_EmoEngineEventCreate();
		Pointer eState				= Edk.INSTANCE.EE_EmoStateCreate();
		IntByReference userID 		= null;
		IntByReference nSamplesTaken= null;
		int state  					= 0;
		float secs 					= 1;
		boolean readytocollect 		= false;
		userID 			= new IntByReference(0);
		nSamplesTaken	= new IntByReference(0);

		if (Edk.INSTANCE.EE_EngineConnect("Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
			System.out.println("Emotiv Engine start up failed.");
			return;
		}

		try {
			Socket clientSocket = new Socket("localhost", 3333); //use socket
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //probably unnecessary 
			//close resources on abrupt program termination
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						clientSocket.close();
						inFromServer.close();
						outToServer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
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
							//System.out.println(nSamplesTaken.getValue()); //debug
							double[] data = new double[nSamplesTaken.getValue()];
							for (int sampleIdx = 0; sampleIdx<nSamplesTaken.getValue(); sampleIdx++) {
								StringJoiner joiner = new StringJoiner(",");
								for (int i = 0; i < 21; i++) { //3-17 are actual sensor values, 21 columns of data total
									Edk.INSTANCE.EE_DataGet(hData, i, data, nSamplesTaken.getValue());
									joiner.add(String.valueOf(data[sampleIdx]));
								}
								String joined = joiner.toString();
								//System.out.println(joined);
								outToServer.writeBytes(joined + '\n');
							}
						}
					}
				}
			}
			Edk.INSTANCE.EE_EngineDisconnect();
			Edk.INSTANCE.EE_EmoStateFree(eState);
			Edk.INSTANCE.EE_EmoEngineEventFree(eEvent);
			System.out.println("Disconnected!");
			clientSocket.close();
			inFromServer.close();
			outToServer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
