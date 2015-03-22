import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.awt.*; 
import java.awt.event.*;
import java.nio.*;

import org.json.JSONObject;

//Main class for controlling the drone with the Emotiv EPOC and Emotiv-JSON API
class APIDrone extends Frame {
	private static final long serialVersionUID = 1L;
	InetAddress inet_addr;
	DatagramSocket socket;
	int seq = 1; //Send AT command with sequence number 1 will reset the counter
	float speed = (float)0.8; //UAV movement speed
	boolean shift = false;
	FloatBuffer fb;
	IntBuffer ib;
	String liftOffParam = "";
	String landParam = "";
	double threshold = 0;

	public APIDrone(String name, String args[]) throws Exception {
		super(name);
		String ip = "192.168.1.1"; //IP address for the UAV WiFi connection
		if (args.length >= 1) {
			ip = args[0];
		}
		StringTokenizer st = new StringTokenizer(ip, ".");
		byte[] ip_bytes = new byte[4];
		if (st.countTokens() == 4) {
			for (int i = 0; i < 4; i++) {
				ip_bytes[i] = (byte)Integer.parseInt(st.nextToken());
			}
		}
		else {
			System.out.println("Incorrect IP address format: " + ip);
			System.exit(-1);
		}
		System.out.println("IP: " + ip);
		System.out.println("Speed: " + speed);
		//connect to drone
		ByteBuffer bb = ByteBuffer.allocate(4);
		fb = bb.asFloatBuffer();
		ib = bb.asIntBuffer();
		inet_addr = InetAddress.getByAddress(ip_bytes);
		socket = new DatagramSocket();
		socket.setSoTimeout(3000);
		send_at_cmd("AT*CONFIG=1,\"control:altitude_max\",\"4000\""); //altitude max 2 meters
		if (args.length == 2) { //Command line mode
			send_at_cmd(args[1]);
			System.exit(0);
		}
		setSize(320, 160);
		setVisible(true);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				//stop on window close, might be good idea to add force land command
				System.out.println("Action: Landing"); 
				try {
					send_at_cmd("AT*REF=" + (seq++) + ",290717696");
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				System.exit(0); 
			}
		});
		Thread t = new Thread(new DroneReset());
		t.start();

		// Connect to API Server on port 4444
		// (NOTE: API server needs to be initially running or connection will be rejected)
		String params;
		String JSONString;
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		Socket clientSocket = new Socket("localhost", 4444);
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		//type in drone control parameters
		System.out.println("Enter EEG event to lift off:");
		liftOffParam = inFromUser.readLine();
		System.out.println("Enter EEG event to land:");
		landParam = inFromUser.readLine();
		System.out.println("Enter threshold value:");
		threshold = Double.parseDouble(inFromUser.readLine());
		//add more later

		params = liftOffParam + landParam;
		System.out.println(params); //debug
		//outToServer.writeBytes(params + '\n');

		//change below while loop, this will end in disaster for a demo
		while ((JSONString = inFromServer.readLine()) != null) {
			JSONObject response = new JSONObject(JSONString);
			control(response);
		}

		//force land command on headset disconnect
		System.out.println("Action: Landing"); 
		send_at_cmd("AT*REF=" + (seq++) + ",290717696");

		//close connections
		clientSocket.close();
		inFromUser.close();
		inFromServer.close();
		outToServer.close();
		System.exit(0); //quit
	}

	public static void main(String args[]) throws Exception {
		new APIDrone("ARDrone MAIN", args);
	}

	public int intOfFloat(float f) {
		fb.put(0, f);
		return ib.get(0);
	}

	//Control AR.Drone via AT commands using the received JSON response
	public void control(JSONObject JSONResponse) throws Exception {
		//parse JSON response here
		double liftOffValue = JSONResponse.optDouble(liftOffParam);
		double landValue = JSONResponse.optDouble(landParam);

		if (liftOffValue > landValue && liftOffValue > threshold) {
			System.out.println("Speed: " + speed);    	
			System.out.println("Action: Takeoff");    	
			send_at_cmd("AT*REF=" + (seq++) + ",290718208");
		}
		else if (landValue > liftOffValue && landValue > threshold){
			System.out.println("Speed: " + speed);    	
			System.out.println("Action: Landing"); 
			send_at_cmd("AT*REF=" + (seq++) + ",290717696");
		}
		/*switch () {
		//Only max of 4 cognitive commands at a time recommended
		//Any changes to letter meaning/action here must occur in EmoKey mapping too
		case 'I':	//Up (lift)
			action = "Go Up (gaz+)";
			at_cmd = "AT*PCMD=" + (seq++) + ",1,0,0," + intOfFloat(speed) + ",0";
			break;
		case 'P':   //Forward (push)
			action = "Go Forward (pitch+)";
			at_cmd = "AT*PCMD=" + (seq++) + ",1,0," + intOfFloat(-speed) + ",0,0";
			break;
		case 'D':	//Down (drop) 
			action = "Go Down (gaz-)";
			at_cmd = "AT*PCMD=" + (seq++) + ",1,0,0," + intOfFloat(-speed) + ",0";
			break;
		case 'U':   //Backward (pull)
			action = "Go Backward (pitch-)";
			at_cmd = "AT*PCMD=" + (seq++) + ",1,0," + intOfFloat(speed) + ",0,0";
			break;
		case 'T':	//turn left (counterclockwise)
			action = "Rotate Left (yaw-)";
			at_cmd = "AT*PCMD=" + (seq++) + ",1,0,0,0," + intOfFloat(-speed);
			break;
		case 'L':   //Left (think left)
			action = "Go Left (roll-)";
			at_cmd = "AT*PCMD=" + (seq++) + ",1," + intOfFloat(-speed) + ",0,0,0";
			break;
		case 'C':	//turn right (clockwise)
			action = "Rotate Right (yaw+)";
			at_cmd = "AT*PCMD=" + (seq++) + ",1,0,0,0," + intOfFloat(speed);
			break;
		case 'R':   //Right (think right)
			action = "Go Right (roll+)";
			at_cmd = "AT*PCMD=" + (seq++) + ",1," + intOfFloat(speed) + ",0,0,0";
			break;
		case 'N':	//Hover (neutral)
			//Not necessary but can be used to stabilize drone 
			action = "Hovering";
			at_cmd = "AT*PCMD=" + (seq++) + ",1,0,0,0,0";
			break;
		case 'S':	//Takeoff (raise brow)
			action = "Takeoff";         
			at_cmd = "AT*REF=" + (seq++) + ",290718208";
			break;
		case 'A':	//Land (clench)
			action = "Landing";
			at_cmd = "AT*REF=" + (seq++) + ",290717696";
			break;
		case 'Z':	//reset 
			action = "Reset";
			at_cmd = "AT*REF=1,290717952";
			break;			
		default:
			break;
		}*/
	}

	public void send_at_cmd(String at_cmd) throws Exception {
		System.out.println("AT command: " + at_cmd);    	
		byte[] buffer = (at_cmd + "\r").getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, inet_addr, 5556);
		socket.send(packet);
		//socket.receive(packet); //AR.Drone does not send back ack message (like "OK")
		//System.out.println(new String(packet.getData(),0,packet.getLength()));   	
	}
}