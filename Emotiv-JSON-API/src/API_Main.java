import java.awt.geom.Point2D;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.function.Function;

import org.json.*;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;

/**
 * 
 * Uses two separate sockets for communication of data between the server and any
 * clients. Port 4444 is for the client to request updates on data and receive those
 * updates in a stream. Port 4445 is for the client to hand training profile loading, 
 * training, and creation for the cognitive commands.
 *
 */

public class API_Main implements Runnable { 
	private static HashMap<Integer, String> cognitivMap;
	private static HashMap<Integer, Boolean> activeCognitivMap;
	private static HashMap<String, Integer> trainingMap;
	private static HashMap<String, Function<Pointer, Float>> affectivMap;
	private static HashMap<String, Function<Pointer, Number>> expressivMap;

	public static void main(String[] args) throws Exception {
		API_Main self= new API_Main();
		self.run();
	}

	public static HashMap<Integer, String> getCogntivMap() {
		return cognitivMap;
	}

	public static HashMap<String, Function<Pointer, Float>> getAffectivMap() {
		return affectivMap;
	}

	public static HashMap<String, Function<Pointer, Number>> getExpressivMap() {
		return expressivMap;
	}

	public static void constructMaps() throws IOException {
		String line = "";
		cognitivMap = new HashMap<Integer, String>();
		trainingMap = new HashMap<String, Integer>();
		activeCognitivMap = new HashMap<Integer, Boolean>();
		affectivMap = new HashMap<String, Function<Pointer, Float>>();
		expressivMap = new HashMap<String, Function<Pointer, Number>>();

		// cognitive
		BufferedReader br = new BufferedReader(
				new InputStreamReader(
						API_Main.class.getResourceAsStream("CognitivEvents.txt")));
		while ((line = br.readLine()) != null) {
			String parts[] = line.split("\t");
			cognitivMap.put(Integer.decode(parts[0]), parts[1]);
		}
		br.close();

		// active cognitive 
		br = new BufferedReader(
				new InputStreamReader(
						API_Main.class.getResourceAsStream("ActiveCognitivEvents.txt")));
		while ((line = br.readLine()) != null) {
			String parts[] = line.split("\t");
			activeCognitivMap.put(Integer.decode(parts[0]), Boolean.valueOf(parts[1]));
		}
		br.close();

		// training
		br = new BufferedReader(
				new InputStreamReader(
						API_Main.class.getResourceAsStream("Training.txt")));
		while ((line = br.readLine()) != null) {
			String parts[] = line.split("\t");
			trainingMap.put(parts[0], Integer.decode(parts[1]));
		}
		br.close();

		// affective
		affectivMap.put("Frustration", EmoState.INSTANCE::ES_AffectivGetFrustrationScore);
		affectivMap.put("Meditation", EmoState.INSTANCE::ES_AffectivGetMeditationScore);
		affectivMap.put("EngagementBoredom", EmoState.INSTANCE::ES_AffectivGetEngagementBoredomScore);
		affectivMap.put("ExcitementShortTerm", EmoState.INSTANCE::ES_AffectivGetExcitementShortTermScore);
		affectivMap.put("ExcitementLongTerm", EmoState.INSTANCE::ES_AffectivGetExcitementLongTermScore);

		// expressive
		expressivMap.put("Blink", EmoState.INSTANCE::ES_ExpressivIsBlink);
		expressivMap.put("LeftWink", EmoState.INSTANCE::ES_ExpressivIsLeftWink);
		expressivMap.put("RightWink", EmoState.INSTANCE::ES_ExpressivIsRightWink);
		expressivMap.put("LookingLeft", EmoState.INSTANCE::ES_ExpressivIsLookingLeft);
		expressivMap.put("LookingRight", EmoState.INSTANCE::ES_ExpressivIsLookingRight);
		expressivMap.put("LookingUp", EmoState.INSTANCE::ES_ExpressivIsLookingUp);
		expressivMap.put("LookingDown", EmoState.INSTANCE::ES_ExpressivIsLookingDown);
		expressivMap.put("EyesOpen", EmoState.INSTANCE::ES_ExpressivIsEyesOpen);
		expressivMap.put("Clench", EmoState.INSTANCE::ES_ExpressivGetClenchExtent);
		expressivMap.put("Smile", EmoState.INSTANCE::ES_ExpressivGetSmileExtent);
		expressivMap.put("EyebrowRaise", EmoState.INSTANCE::ES_ExpressivGetEyebrowExtent);
	}

	public static void startTrainingCognitiv(Integer cognitivAction, Pointer eEvent) {
		UpgradedEdk.INSTANCE.IEE_MentalCommandSetTrainingAction(0, cognitivAction);
		UpgradedEdk.INSTANCE.IEE_MentalCommandSetTrainingControl(0,
				UpgradedEdk.IEE_MentalCommandTrainingControl_t.MC_START.getType());
	}

	public static void enableCognitivActionsList(Pointer eEvent) {
		long cognitivActions = 0x0000;
		for (Entry<Integer, Boolean> entry : activeCognitivMap.entrySet()) {
			if (entry.getValue() && entry.getKey() != EmoState.EE_CognitivAction_t.COG_NEUTRAL.ToInt()) {
				cognitivActions = cognitivActions | entry.getKey();
			}
		}
		UpgradedEdk.INSTANCE.IEE_MentalCommandSetActiveActions(0, cognitivActions);     
	}

	public static void handleTraining(BufferedReader cogProfileInFromClient, DataOutputStream cogProfileOutToClient, Pointer eEvent) {
		String strRequestCogProfile = "";
		while(true) {
			try {
				while (cogProfileInFromClient.ready()) {
					strRequestCogProfile = cogProfileInFromClient.readLine();
					System.out.println(strRequestCogProfile);
					if (strRequestCogProfile.equals("save")) {
						EmoProfileManagement.SaveCurrentProfile();
						EmoProfileManagement.SaveProfilesToFile();
					}
					else if (strRequestCogProfile.contains("train")) {
						String command = strRequestCogProfile.substring(strRequestCogProfile.lastIndexOf(" ") + 1);
						if (strRequestCogProfile.contains("neutral")) {
							UpgradedEdk.INSTANCE.IEE_MentalCommandSetTrainingAction(0,
									EmoState.EE_CognitivAction_t.COG_NEUTRAL.ToInt());
							UpgradedEdk.INSTANCE.IEE_MentalCommandSetTrainingControl(0,
									UpgradedEdk.IEE_MentalCommandTrainingControl_t.MC_START.getType());
						}
						else if (trainingMap.containsKey(command)) {
							enableCognitivActionsList(eEvent); // might be causing issues, will comment out until confirmed
							startTrainingCognitiv(trainingMap.get(command), eEvent);
						}
					}
					else if (strRequestCogProfile.contains("set")){
						// must each be between 1 and 10
						if (strRequestCogProfile.contains("command sensitivity")) {
							String[] strnums = strRequestCogProfile.replace("set command sensitivity ", "").split(", ");
							UpgradedEdk.INSTANCE.IEE_MentalCommandSetActionSensitivity(0, 
									Integer.parseInt(strnums[0]),
									Integer.parseInt(strnums[1]),
									Integer.parseInt(strnums[2]),
									Integer.parseInt(strnums[3]));
						}
						else if (strRequestCogProfile.contains("overall sensitivity")) {
							// must be between 1 and 7
							System.out.println(strRequestCogProfile.replace("set overall sensitivity ", ""));
							UpgradedEdk.INSTANCE.IEE_MentalCommandSetActivationLevel(0, 
									Integer.parseInt(strRequestCogProfile.replace("set overall sensitivity ", "")));
						}
					}
					else if (strRequestCogProfile.contains("get")){
						if (strRequestCogProfile.contains("command sensitivity")) {
							IntByReference level1 = new IntByReference(0);
							IntByReference level2 = new IntByReference(0);
							IntByReference level3 = new IntByReference(0);
							IntByReference level4 = new IntByReference(0);
							UpgradedEdk.INSTANCE.IEE_MentalCommandGetActionSensitivity(0, 
									level1, level2, level3, level4);
							cogProfileOutToClient.writeBytes(
									level1.getValue() + ", " +
									level2.getValue() + ", " +
									level3.getValue() + ", " +
									level4.getValue() + "\n");
						}
						else if (strRequestCogProfile.contains("overall sensitivity")) {
							IntByReference level = new IntByReference(0);
							UpgradedEdk.INSTANCE.IEE_MentalCommandGetActivationLevel(0, level);
							cogProfileOutToClient.writeBytes(level.getValue() + "\n");
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		ServerSocket serverSocket, serverCogProfileSocket;
		try {
			serverSocket 			= new ServerSocket(4444); // EEG data socket
			serverCogProfileSocket 	= new ServerSocket(4445); // training profile socket
			Pointer eEvent			= UpgradedEdk.INSTANCE.IEE_EmoEngineEventCreate();
			Pointer eState			= UpgradedEdk.INSTANCE.IEE_EmoStateCreate();
			IntByReference userID 	= new IntByReference(0);
			IntByReference pXOut	= new IntByReference(0);
			IntByReference pYOut	= new IntByReference(0);
			// based on Emotiv library, may or may not be able to access other motion data
			// besides X and Y gyros, so below lines are commented out for now
			//Pointer hMotionData 	= UpgradedEdk.INSTANCE.IEE_MotionDataCreate();
			//IntByReference nSamplesTaken = new IntByReference(0);
			//IntByReference contactQuality = new IntByReference(0);
			int state = 0;
			String strParams = "";
			HashSet<String> params = new HashSet<String>(); //use HashSet for O(1) lookup time
			String[] wirelessSignalStatus = {"NO_SIGNAL", "BAD_SIGNAL", "GOOD_SIGNAL"};

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						serverSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});

			constructMaps();

			if (UpgradedEdk.INSTANCE.IEE_EngineConnect("Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
				System.out.println("Emotiv Engine start up failed.");
				return;
			}

			while (true) {
				//will block, waiting for a client to send data to before it reads EEG data
				try {
					Socket connectionSocket = serverSocket.accept();
					Socket trainingDataSocket = serverCogProfileSocket.accept(); // must be connected to both sockets to proceed

					BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
					DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

					BufferedReader cogProfileInFromClient = new BufferedReader(new InputStreamReader(trainingDataSocket.getInputStream()));
					DataOutputStream cogProfileOutToClient = new DataOutputStream(trainingDataSocket.getOutputStream());				

					// handle training profile requests (from 4445) in a separate thread
					new Thread(() -> handleTraining(cogProfileInFromClient, cogProfileOutToClient, eEvent)).start();

					// handle EEG data transmission (to 4444) and training profile responses (to 4445)
					// client tells server what events it wants to be updated about
					strParams = inFromClient.readLine();

					//get each param from comma separated input and add them to the HashSet
					StringTokenizer st = new StringTokenizer(strParams, ", ");
					while(st.hasMoreTokens())
						params.add(st.nextToken());
					System.out.println("Client wants updates on: " + params.toString()); 

					// rezero the gyro here
					if (UpgradedEdk.INSTANCE.IEE_HeadsetGyroRezero(0) == EdkErrorCode.EDK_OK.ToInt()) 
						System.out.println("OK"); //0 should be the proper ID since only one headset
					
					//concatenated response of headset data and EEG events
					JSONObject response = new JSONObject(); 
					JSONObject headsetData = new JSONObject();
					//EEG Events
					JSONObject emoStateData = new JSONObject();
					JSONObject expressivData = new JSONObject();
					JSONObject affectivData = new JSONObject();
					JSONObject gyros = new JSONObject();
					while (true) { 

						state = UpgradedEdk.INSTANCE.IEE_EngineGetNextEvent(eEvent);
						// New event needs to be handled
						if (state == EdkErrorCode.EDK_OK.ToInt()) {							
							if (params.contains("gyros") || params.contains("motion") || params.contains("*")) {
								//seem to have a max of +/- 15000
								UpgradedEdk.INSTANCE.IEE_HeadsetGetGyroDelta(0, pXOut, pYOut);
								gyros.put("GyroX", pXOut.getValue());
								gyros.put("GyroY", pYOut.getValue());
								emoStateData.put("Gyros", gyros);
							}

							int eventType = UpgradedEdk.INSTANCE.IEE_EmoEngineEventGetType(eEvent);
							UpgradedEdk.INSTANCE.IEE_EmoEngineEventGetUserId(eEvent, userID);

							if (eventType == UpgradedEdk.IEE_Event_t.IEE_UserAdded.ToInt()) {
								cogProfileOutToClient.writeBytes("Enter username: \n");
								String user = cogProfileInFromClient.readLine();
								System.out.println("User: " + user);
								EmoProfileManagement.AddNewProfile(user);
								String actionList = EmoProfileManagement.CheckCurrentProfile();
							     long cognitivActions = Long.valueOf(actionList);
							     System.out.println(cognitivActions);
							     UpgradedEdk.INSTANCE.IEE_MentalCommandSetActiveActions(0, cognitivActions);
							}

							if (eventType == UpgradedEdk.IEE_Event_t.IEE_MentalCommandEvent.ToInt()) {
								int cogType = UpgradedEdk.INSTANCE.IEE_MentalCommandEventGetType(eEvent);
								if(cogType == UpgradedEdk.IEE_MentalCommandEvent_t.IEE_MentalCommandTrainingStarted.getType()) {
									cogProfileOutToClient.writeBytes("Cognitive training started.\n");
								}
								else if(cogType == UpgradedEdk.IEE_MentalCommandEvent_t.IEE_MentalCommandTrainingCompleted.getType()) {
									cogProfileOutToClient.writeBytes("Cognitive training complete.\n");
								}
								else if(cogType == UpgradedEdk.IEE_MentalCommandEvent_t.IEE_MentalCommandTrainingSucceeded.getType()) {
									UpgradedEdk.INSTANCE.IEE_MentalCommandSetTrainingControl(0,
											UpgradedEdk.IEE_MentalCommandTrainingControl_t.MC_ACCEPT.getType());
									cogProfileOutToClient.writeBytes("Cognitive training succeeded.\n");
								}
								else if(cogType == UpgradedEdk.IEE_MentalCommandEvent_t.IEE_MentalCommandTrainingFailed.getType()) {
									cogProfileOutToClient.writeBytes("Cognitive training failed.\n");
								}
								else if(cogType == UpgradedEdk.IEE_MentalCommandEvent_t.IEE_MentalCommandTrainingRejected.getType()) {
									cogProfileOutToClient.writeBytes("Cognitive training rejected.\n");
								}
							}

							if (eventType == UpgradedEdk.IEE_Event_t.IEE_EmoStateUpdated.ToInt()) {
								UpgradedEdk.INSTANCE.IEE_EmoEngineEventGetEmoState(eEvent, eState);
								headsetData.put("Timestamp", EmoState.INSTANCE.ES_GetTimeFromStart(eState));
								headsetData.put("EmoState", userID.getValue());
								headsetData.put("WirelessSignalStatus", wirelessSignalStatus[EmoState.INSTANCE.ES_GetWirelessSignalStatus(eState)]);
								headsetData.put("IsHeadsetOn", EmoState.INSTANCE.ES_GetHeadsetOn(eState));
								IntByReference batteryLevel = new IntByReference(0);
								IntByReference maxBatteryLevel = new IntByReference(0);
								EmoState.INSTANCE.ES_GetBatteryChargeLevel(eState, batteryLevel, maxBatteryLevel);
								headsetData.put("BatteryChargeLevel", batteryLevel.getValue());
								response.put("HeadsetData", headsetData);
								for (Entry<String, Function<Pointer, Number>> entry : expressivMap.entrySet()) {
									if (params.contains(entry.getKey()) || params.contains("*") || params.contains("expressive")) {
										float returnVal = (entry.getValue().apply(eState)).floatValue();
										if (returnVal > 0) {
											expressivData.put(entry.getKey(), returnVal);
										}
									}	
								}
								if (params.contains("EyeLocation") || params.contains("*") || params.contains("expressive")) {
									FloatByReference eyeXLocation = new FloatByReference(0);
									FloatByReference eyeYLocation = new FloatByReference(0);
									EmoState.INSTANCE.ES_ExpressivGetEyeLocation(eState, eyeXLocation, eyeYLocation);
									Point2D.Float eyeLocation = new Point2D.Float(eyeXLocation.getValue(),eyeYLocation.getValue());
									if (eyeXLocation.getValue() != 0.0 && eyeYLocation.getValue() != 0.0) {
										expressivData.put("EyeLocation", eyeLocation);
									}
								}
								emoStateData.put("Expressive", expressivData);

								for (Entry<String, Function<Pointer, Float>> entry : affectivMap.entrySet()) {
									if (params.contains(entry.getKey()) || params.contains("*") || params.contains("affective")) {
										affectivData.put(entry.getKey(), (entry.getValue().apply(eState)).floatValue());
									}	
								}
								emoStateData.put("Affective", affectivData);
								
								JSONObject cognitivData = new JSONObject();
								if (params.contains("cognitive") || params.contains("*")) {
									int currentAction = EmoState.INSTANCE.ES_CognitivGetCurrentAction(eState);
									if (cognitivMap.containsKey(currentAction))
										cognitivData.put(cognitivMap.get(currentAction), EmoState.INSTANCE.ES_CognitivGetCurrentActionPower(eState));
								}
								emoStateData.put("Cognitive", cognitivData);
								response.put("EmoStateData", emoStateData);
								outToClient.writeBytes(response.toString() + "\n"); //write to socket only at end*/
							}
						}
						else if (state != EdkErrorCode.EDK_NO_EVENT.ToInt()) {
							outToClient.writeBytes("Internal error in Emotiv Engine!");
							break;
						}
					}
					UpgradedEdk.INSTANCE.IEE_EngineDisconnect();
					UpgradedEdk.INSTANCE.IEE_EmoStateFree(eState);
					UpgradedEdk.INSTANCE.IEE_EmoEngineEventFree(eEvent);
					System.out.println("Disconnected!");
				}
				catch (SocketException se) {
					System.out.println("Socket error");
					se.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}