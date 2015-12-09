import java.io.*;
import java.net.Socket;
import org.json.*;

public class NiceCode {
	public static void main(String[] args) {
		String JSONResponse = "";
		try {
			Socket clientSocket = new Socket("localhost", 4444);
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			outToServer.writeBytes("expressive" + '\n');
			while ((JSONResponse = inFromServer.readLine()) != null) {
				JSONObject obj = new JSONObject(JSONResponse);
				JSONObject data = obj.getJSONObject("EmoStateData").getJSONObject("Expressive");
				System.out.print(
						data.toString().replaceAll("[\\{\"\\}]", "").replaceAll(",", "\n").replaceAll(
								":", ", "));
			}
			clientSocket.close();
			inFromServer.close();
			outToServer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}