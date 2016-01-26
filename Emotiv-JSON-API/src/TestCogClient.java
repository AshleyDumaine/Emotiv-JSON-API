import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import org.json.*;

public class TestCogClient {
	public static void main(String[] args) {
		String JSONResponse = "";
		try {
			Socket clientSocket = new Socket("localhost", 4444);
			Socket clientTrainingSocket = new Socket("localhost", 4445);
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			DataOutputStream trainingOutToServer = new DataOutputStream(clientTrainingSocket.getOutputStream());
			BufferedReader trainingInFromServer = new BufferedReader(new InputStreamReader(clientTrainingSocket.getInputStream()));
			outToServer.writeBytes("cognitive" + '\n');
			new Thread(() -> handleTraining(trainingInFromServer, trainingOutToServer)).start();
			while ((JSONResponse = inFromServer.readLine()) != null) {
				JSONObject obj = new JSONObject(JSONResponse);
				/*System.out.println(obj.toString());
				JSONObject data = obj.getJSONObject("EmoStateData").getJSONObject("Cognitive");
				System.out.println(
						data.toString().replaceAll("[\\{\"\\}]", "").replaceAll(",", "\n").replaceAll(
								":", " "));*/
			}
			clientSocket.close();
			inFromServer.close();
			outToServer.close();
			clientTrainingSocket.close();
			trainingOutToServer.close();
			trainingInFromServer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void handleTraining(BufferedReader trainingInFromServer, DataOutputStream trainingOutToServer) {
		String response = "";
		InputStreamReader fileInputStream = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(fileInputStream);
		while(true) {
			try {
				while (trainingInFromServer.ready()) {
					response = trainingInFromServer.readLine();
					System.out.println(response);
					if (response.contains("Enter username:")) {
						trainingOutToServer.writeBytes("Jon\n");
					}
				}
				while (br.ready()){
					trainingOutToServer.writeBytes(br.readLine() + '\n');
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}