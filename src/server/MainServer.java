package server;

import java.io.IOException;
import java.net.ServerSocket;

public class MainServer {

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Invalid arguments");
			System.exit(1);
		}

		int portNumber = Integer.parseInt(args[0]);
		boolean listening = true;

		try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
			while (listening)
				new ServerMultiThread(serverSocket.accept()).start();
		} catch (IOException e) {
			System.out.println(
					"Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
			System.out.println(e.getMessage());
		}
	}

}
