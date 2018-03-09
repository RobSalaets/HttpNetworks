package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient{
	
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	public ChatClient() {
		
	}
	
	public void connect(String host, int port) {
		try{
			socket = new Socket(host, port);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
		}catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public void post(String command, String host, String uri, String httpNumber) {
		System.out.println(command + " " + uri + " " + httpNumber);
		out.println(command + " " + uri + " " + httpNumber);
		out.println("Host: " + host);
		out.println();
	}
	
	public void poll(String filename) {
		try{
			String data = in.readLine();
			if(data != null) {
				PrintWriter writer = new PrintWriter(filename, "UTF-8");
				boolean htmlLines = false;
				while(in.ready()) {
					if(data.equals("<!doctype html>"))
						htmlLines = true;
					if(htmlLines)
						writer.println(data);
					data = in.readLine();
					System.out.println(data);
				}
				writer.close();
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		try{
			socket.close();
			in.close();
			out.close();
		}catch (IOException e){
			e.printStackTrace();
		}
	}
}
