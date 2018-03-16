package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerMultiThread extends Thread {
	private Socket socket = null;
	
	public ServerMultiThread(Socket socket) {
		super("ServerMutliThread");
		this.socket = socket;
	}
	
	public void run() {
		try (
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));				
		) {
			String inputLine = null, outputLine;
			Server server = new Server();
			
			// TODO: input
			outputLine = server.processInput(inputLine);
			out.println(outputLine);
			
			while ((inputLine = in.readLine()) != null) {
				outputLine = server.processInput(inputLine);
				out.println(outputLine);
				
				// TODO: Handle when to end server
				if (outputLine != null && outputLine.equals("exit"))
					break;
			}
			socket.close();
		} catch(IOException e) {
			System.out.println("Exception caught when trying to connect to the server. Possibly client disconnected from server");
			System.out.println(e.getMessage());
		}
	}

}
