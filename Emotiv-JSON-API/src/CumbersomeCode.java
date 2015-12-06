import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.Function;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public class CumbersomeCode { 
	private static HashMap<String, Function<Pointer, Number>> expressivMap = new HashMap<String, Function<Pointer, Number>>();
	public static void main(String[] args) {
		Pointer eEvent			= Edk.INSTANCE.EE_EmoEngineEventCreate();
		Pointer eState			= Edk.INSTANCE.EE_EmoStateCreate();
		IntByReference userID 	= new IntByReference(0);
		int state = 0;

		constructMap();

		if (Edk.INSTANCE.EE_EngineConnect("Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
			System.out.println("Emotiv Engine start up failed.");
			return;
		}
		while (true) { 
			state = Edk.INSTANCE.EE_EngineGetNextEvent(eEvent);
			// New event needs to be handled
			if (state == EdkErrorCode.EDK_OK.ToInt()) {
				int eventType = Edk.INSTANCE.EE_EmoEngineEventGetType(eEvent);
				Edk.INSTANCE.EE_EmoEngineEventGetUserId(eEvent, userID);
				if (eventType == Edk.EE_Event_t.EE_EmoStateUpdated.ToInt()) {
					Edk.INSTANCE.EE_EmoEngineEventGetEmoState(eEvent, eState);
					for (Entry<String, Function<Pointer, Number>> entry : expressivMap.entrySet()) {
						float returnVal = (entry.getValue().apply(eState)).floatValue();
						if (returnVal > 0) {
							System.out.printf("%s, %f\n", entry.getKey(), returnVal);
						}
					}
				}
			}
			else if (state != EdkErrorCode.EDK_NO_EVENT.ToInt()) {
				System.out.println("Internal error in Emotiv Engine!");
				break;
			}
		}
		Edk.INSTANCE.EE_EngineDisconnect();
		System.out.println("Disconnected!");
	}

	public static void constructMap() {
		expressivMap = new HashMap<String, Function<Pointer, Number>>();
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
}