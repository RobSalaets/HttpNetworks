package server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class ServerMultiThread extends Thread {
	private Socket socket = null;

	public ServerMultiThread(Socket socket) {
		super("ServerMultiThread");
		this.socket = socket;
	}

	public void run() {
		try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				BufferedInputStream in = new BufferedInputStream(socket.getInputStream())) {
			Server server = new Server();

			while (!server.needToClose()) {
				server.processInput(in);
				if(!server.isDisconnected())
					out.println(server.getOutput());
				server.printLog(String.valueOf(this.getId()));
			}
			
			socket.close();
		} catch(SocketException e) {
			System.out.println(String.valueOf(this.getId()) + ": Client disconnected from server");
		}catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}
