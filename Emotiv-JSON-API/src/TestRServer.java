import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.io.*;

public class TestRServer {
	public static void main(String argv[]) throws Exception
	{
		ServerSocket serverSocket = new ServerSocket(3333);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		try {
			Socket connectionSocket = serverSocket.accept();
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			while(true) {
				System.out.println(inFromClient.readLine());
			}
		}
		catch (SocketException se) {
			System.out.println("Socket error");
			se.printStackTrace();
		}
	}
}
