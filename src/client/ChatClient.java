package client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;


public class ChatClient{
	
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	public ChatClient() {
		
	}
	
	public void connect(String host, int port) {
		try{
			socket = new Socket(host, port);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ISO-8859-1"));
			out = new PrintWriter(socket.getOutputStream(), true);
		}catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public void httpCommand(String command, String host, String resource, String httpNumber) {
		System.out.println(command + " " + resource + " " + httpNumber);
		System.out.println("Host: " + host);
		out.println(command + " " + resource + " " + httpNumber);
		out.println("Host: " + host);
		out.println();
	}
	
	public boolean pollForResource(String imageResource) {
		try{
			String line = in.readLine();
			if(line != null) {
				System.out.println(line + " for resource: " + imageResource);
				File file = new File(imageResource);
				if(imageResource.contains("/"))
					file.getParentFile().mkdirs();
				file.createNewFile();
				FileOutputStream binWriter = new FileOutputStream(file);
				PrintWriter writer = new PrintWriter(file, "UTF-8");
				boolean dataLines = false;
				boolean binary= false;
				StringBuffer data = new StringBuffer();
				while(in.ready()) {
					if(dataLines) {
						if(!binary)
							writer.println(line);
						else {
							data.append(line + "\n");
						}
					}else
						System.out.println(line);
					
					if(line.equals("")) 
						dataLines = true;
					if(line.toLowerCase().contains("content-type:") && !binary && line.toLowerCase().contains("image"))
						binary = true;
					
					line = in.readLine();
				}
				if(binary)
					binWriter.write(data.toString().getBytes("ISO-8859-1"));
				writer.close();
				binWriter.close();
				return false;
				
			}
		}catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
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
