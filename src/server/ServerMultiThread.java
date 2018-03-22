package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class ServerMultiThread extends Thread {
	private Socket socket = null;

	public ServerMultiThread(Socket socket) {
		super("ServerMultiThread");
		this.socket = socket;
	}

	public void run() {
		try (BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
				BufferedInputStream in = new BufferedInputStream(socket.getInputStream())) {
			Server server = new Server();

			while (!server.needToClose()) {
				server.processInput(in);
				if(!server.isDisconnected()) {
					String output = server.getOutput();
					out.write(output.getBytes(), 0, output.length());
					byte[] data = server.getContent();
					if(data != null) {
						out.write(data, 0, data.length);
					}
					out.flush();
					
				}
				
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
