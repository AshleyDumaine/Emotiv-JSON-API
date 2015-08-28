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

public class API_Main implements Runnable { 
	private static HashMap<Integer, String> cognitivMap = new HashMap<Integer, String>();
	private static HashMap<String, Function<Pointer, Float>> affectivMap = new HashMap<String, Function<Pointer, Float>>();
	private static HashMap<String, Function<Pointer, Number>> expressivMap = new HashMap<String, Function<Pointer, Number>>();
	public static void main(String[] args) throws Exception {
		API_Main self = new API_Main();
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
		affectivMap = new HashMap<String, Function<Pointer, Float>>();
		expressivMap = new HashMap<String, Function<Pointer, Number>>();

		//cognitiv
		BufferedReader br = new BufferedReader(new FileReader((new File("CognitivEvents.txt")).getAbsolutePath()));
		while ((line = br.readLine()) != null) {
			String parts[] = line.split("\t");
			cognitivMap.put(Integer.decode(parts[0]), parts[1]);
		}
		br.close();

		//affectiv
		affectivMap.put("Frustration", EmoState.INSTANCE::ES_AffectivGetFrustrationScore);
		affectivMap.put("Meditation", EmoState.INSTANCE::ES_AffectivGetMeditationScore);
		affectivMap.put("EngagementBoredom", EmoState.INSTANCE::ES_AffectivGetEngagementBoredomScore);
		affectivMap.put("ExcitementShortTerm", EmoState.INSTANCE::ES_AffectivGetExcitementShortTermScore);
		affectivMap.put("ExcitementLongTerm", EmoState.INSTANCE::ES_AffectivGetExcitementLongTermScore);

		//expressiv
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

	@Override
	public void run() {
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(4444);
			Pointer eEvent			= Edk.INSTANCE.EE_EmoEngineEventCreate();
			Pointer eState			= Edk.INSTANCE.EE_EmoStateCreate();
			IntByReference userID 	= new IntByReference(0);
			IntByReference pXOut	= new IntByReference(0);
			IntByReference pYOut	= new IntByReference(0);
			//IntByReference contactQuality = new IntByReference(0);
			IntByReference chargeLevel = new IntByReference(0);
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

			if (Edk.INSTANCE.EE_EngineConnect("Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
				System.out.println("Emotiv Engine start up failed.");
				return;
			}


			while (true) {
				//will block, waiting for a client to send data to before it reads EEG data
				try {
					Socket connectionSocket = serverSocket.accept();
					BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
					DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

					// client tells server what events it wants to be updated about
					strParams = inFromClient.readLine();

					//get each param from comma separated input and add them to the HashSet
					StringTokenizer st = new StringTokenizer(strParams, ", ");
					while(st.hasMoreTokens())
						params.add(st.nextToken());
					System.out.println("Client wants updates on: " + params.toString()); 


					//put stuff here?
					//Edk.INSTANCE.EE_EngineRemoteConnect("127.0.0.1", (short) 0, "Emotiv Systems-5");
					//probably rezero the gyro here....
					if (Edk.INSTANCE.EE_HeadsetGyroRezero(0) == EdkErrorCode.EDK_OK.ToInt()) System.out.println("OK"); //0 should be the proper ID since only one headset
					while (true) { 
						//should all this be here?
						//concatenated response of headset data and EEG events
						JSONObject response = new JSONObject(); 
						JSONObject headsetData = new JSONObject();
						//EEG Events
						JSONObject emoStateData = new JSONObject();
						JSONArray expressivArray = new JSONArray();
						JSONArray affectivArray = new JSONArray();
						JSONArray gyroArray = new JSONArray(); //NEW
						JSONObject gyroX = new JSONObject();
						JSONObject gyroY = new JSONObject();

						state = Edk.INSTANCE.EE_EngineGetNextEvent(eEvent);
						// New event needs to be handled
						if (state == EdkErrorCode.EDK_OK.ToInt()) {

							//Put gyro stuff here?
							if (params.contains("Gyros")) { //seem to have a max of +/- 15000
								Edk.INSTANCE.EE_HeadsetGetGyroDelta(0, pXOut, pYOut);
								gyroX.put("GyroX", pXOut.getValue());
								gyroY.put("GyroY", pYOut.getValue());
								gyroArray.put(gyroX);
								gyroArray.put(gyroY);
								emoStateData.put("Gyros", gyroArray);
							}
							
							
							int eventType = Edk.INSTANCE.EE_EmoEngineEventGetType(eEvent);
							Edk.INSTANCE.EE_EmoEngineEventGetUserId(eEvent, userID);
							// Log the EmoState if it has been updated
							if (eventType == Edk.EE_Event_t.EE_EmoStateUpdated.ToInt()) {
								Edk.INSTANCE.EE_EmoEngineEventGetEmoState(eEvent, eState);
								headsetData.put("Timestamp", EmoState.INSTANCE.ES_GetTimeFromStart(eState));
								headsetData.put("EmoState", userID.getValue());
								headsetData.put("WirelessSignalStatus", wirelessSignalStatus[EmoState.INSTANCE.ES_GetWirelessSignalStatus(eState)]);
								//headsetData.put("SensorContactQuality", EmoState.INSTANCE.ES_GetContactQualityFromAllChannels(eState, contactQuality, numChannels));
								headsetData.put("IsHeadsetOnCorrectly", EmoState.INSTANCE.ES_GetHeadsetOn(eState));
								IntByReference maxChargeLevel = new IntByReference(); //maxChargeLevel = ???
								EmoState.INSTANCE.ES_GetBatteryChargeLevel(eState, chargeLevel, maxChargeLevel);
								headsetData.put("BatteryChargeLevel", chargeLevel.getValue());
								response.put("HeadsetData", headsetData);
								for (Entry<String, Function<Pointer, Number>> entry : expressivMap.entrySet()) {
									if (params.contains(entry.getKey())) {
										float returnVal = (entry.getValue().apply(eState)).floatValue();
										if (returnVal > 0) {
											JSONObject jo = new JSONObject();
											jo.put(entry.getKey(), returnVal);
											expressivArray.put(jo);
										}
									}	
								}
								if (params.contains("EyeLocation")) {
									JSONObject jo = new JSONObject();
									FloatByReference eyeXLocation = new FloatByReference(0);
									FloatByReference eyeYLocation = new FloatByReference(0);
									EmoState.INSTANCE.ES_ExpressivGetEyeLocation(eState, eyeXLocation, eyeYLocation);
									Point2D.Float eyeLocation = new Point2D.Float(eyeXLocation.getValue(),eyeYLocation.getValue());
									jo.put("EyeLocation", eyeLocation); 
									expressivArray.put(jo);
								}
								emoStateData.put("Expressiv", expressivArray);

								for (Entry<String, Function<Pointer, Float>> entry : affectivMap.entrySet()) {
									if (params.contains(entry.getKey())) {
										JSONObject jo = new JSONObject();
										jo.put(entry.getKey(), (entry.getValue().apply(eState)).floatValue());
										affectivArray.put(jo);
									}	
								}
								emoStateData.put("Affectiv", affectivArray);

								JSONObject cognitivData = new JSONObject();
								if (params.contains("Cognitiv")) {
									int currentAction = EmoState.INSTANCE.ES_CognitivGetCurrentAction(eState);
									if (cognitivMap.containsKey(currentAction))
										cognitivData.put(cognitivMap.get(currentAction), EmoState.INSTANCE.ES_CognitivGetCurrentActionPower(eState));
								}
								emoStateData.put("Cognitiv", cognitivData);
								response.put("EmoStateData", emoStateData);
								outToClient.writeBytes(response.toString() + "\n"); //write to socket only at end
							}
						}
						else if (state != EdkErrorCode.EDK_NO_EVENT.ToInt()) {
							outToClient.writeBytes("Internal error in Emotiv Engine!");
							break;
						}
					}
					Edk.INSTANCE.EE_EngineDisconnect();
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