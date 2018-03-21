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
		try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
			String inputLine;
			Server server = new Server();

			while (!server.needToClose()) {
				inputLine = in.readLine();
				if(inputLine != null) {
					while(in.ready()) {
						server.processInput(inputLine);
						inputLine = in.readLine();
					}
					out.println(server.getOutput());
				}
			}
			
			socket.close();
		} catch (IOException e) {
			System.out.println("Client disconnected from server.");
			System.out.println(e.getMessage());
		}
	}

}
