import java.net.Socket;
import java.io.*;

import org.json.JSONObject;

public class TestClient {
	public static void main(String argv[]) throws Exception
	{
		String params;
		String JSONResponse;
		BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
		Socket clientSocket = new Socket("localhost", 2222);
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		params = inFromUser.readLine();
		outToServer.writeBytes(params + '\n');
		while ((JSONResponse = inFromServer.readLine()) != null) {
			JSONObject obj = new JSONObject(JSONResponse);
			System.out.println(obj);
		}
		clientSocket.close();
		inFromUser.close();
		inFromServer.close();
		outToServer.close();
	}
}
