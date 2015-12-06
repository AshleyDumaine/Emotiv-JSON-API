import java.awt.geom.Point2D;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.function.Function;

import org.json.*;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;

public class API_Mocker implements Runnable { 
	private static HashMap<Integer, String> cognitivMap = new HashMap<Integer, String>();
	private static HashMap<String, Function<Pointer, Float>> affectivMap = new HashMap<String, Function<Pointer, Float>>();
	private static HashMap<String, Function<Pointer, Number>> expressivMap = new HashMap<String, Function<Pointer, Number>>();
	public static void main(String[] args) throws Exception {
		API_Mocker self= new API_Mocker();
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
		//BufferedReader br = new BufferedReader(new FileReader((new File("CognitivEvents.txt")).getAbsolutePath()));
		BufferedReader br = new BufferedReader(
				new InputStreamReader(
						API_Main.class.getResourceAsStream("CognitivEvents.txt")));
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

					while (true) { 
						//concatenated response of headset data and EEG events
						JSONObject response = new JSONObject(); 
						JSONObject headsetData = new JSONObject();
						//EEG Events
						JSONObject emoStateData = new JSONObject();
						JSONArray expressivArray = new JSONArray();
						JSONArray affectivArray = new JSONArray();
						JSONArray gyroArray = new JSONArray();
						JSONObject gyroX = new JSONObject();
						JSONObject gyroY = new JSONObject();
						Random generator = new Random();

						if (params.contains("gyros") || params.contains("*")) { //seem to have a max of +/- 15000
							gyroX.put("GyroX", generator.nextFloat());
							gyroY.put("GyroY", generator.nextFloat());
							gyroArray.put(gyroX);
							gyroArray.put(gyroY);
							emoStateData.put("Gyros", gyroArray);
						}

						java.util.Date date= new java.util.Date();
						headsetData.put("Timestamp", date.getTime());
						headsetData.put("EmoState", 0);
						headsetData.put("WirelessSignalStatus", wirelessSignalStatus[2]);
						//headsetData.put("SensorContactQuality", EmoState.INSTANCE.ES_GetContactQualityFromAllChannels(eState, contactQuality, numChannels));
						headsetData.put("IsHeadsetOnCorrectly", 1);
						headsetData.put("BatteryChargeLevel", 3);
						response.put("HeadsetData", headsetData);
						for (Entry<String, Function<Pointer, Number>> entry : expressivMap.entrySet()) {
							if (params.contains(entry.getKey()) || params.contains("*") || params.contains("expressive")) {
								float returnVal = generator.nextFloat();
								if (returnVal > 0) {
									JSONObject jo = new JSONObject();
									jo.put(entry.getKey(), returnVal);
									expressivArray.put(jo);
								}
							}	
						}
						if (params.contains("EyeLocation") || params.contains("*") || params.contains("expressive")) {
							JSONObject jo = new JSONObject();
							FloatByReference eyeXLocation = new FloatByReference(0);
							FloatByReference eyeYLocation = new FloatByReference(0);
							Point2D.Float eyeLocation = new Point2D.Float(generator.nextFloat(),generator.nextFloat());
							if (eyeXLocation.getValue() != 0.0 && eyeYLocation.getValue() != 0.0) {
								jo.put("EyeLocation", eyeLocation); 
								expressivArray.put(jo);
							}
						}
						emoStateData.put("Expressive", expressivArray);

						for (Entry<String, Function<Pointer, Float>> entry : affectivMap.entrySet()) {
							if (params.contains(entry.getKey()) || params.contains("*") || params.contains("affective")) {
								JSONObject jo = new JSONObject();
								jo.put(entry.getKey(), generator.nextFloat());
								affectivArray.put(jo);
							}	
						}
						emoStateData.put("Affective", affectivArray);

						JSONObject cognitivData = new JSONObject();
						if (params.contains("cognitive") || params.contains("*")) {
							int currentAction = 0;
							if (cognitivMap.containsKey(currentAction))
								cognitivData.put(cognitivMap.get(currentAction), generator.nextFloat());
						}
						emoStateData.put("Cognitive", cognitivData);
						response.put("EmoStateData", emoStateData);
						outToClient.writeBytes(response.toString() + "\n"); //write to socket only at end
					}
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