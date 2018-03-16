package client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

public class ChatClient{

	private Socket socket;
	private BufferedInputStream in;
	private PrintWriter out;
	private File responseLog;
	private PrintWriter logWriter;
	private String currentHostHttp;

	public ChatClient(){
		try{
			responseLog = new File("./headerLog.txt");
			responseLog.createNewFile();
			logWriter = new PrintWriter(responseLog, "UTF-8");
			currentHostHttp = "";
		}catch (IOException e){
			e.printStackTrace();
		}
	}

	public void connect(String host, int port){
		try{
			socket = new Socket(host, port);
			in = new BufferedInputStream(socket.getInputStream());
			out = new PrintWriter(socket.getOutputStream(), true);
			currentHostHttp = "";
		}catch (IOException e){
			e.printStackTrace();
		}
	}

	public void httpCommand(String command, String host, String resource, String httpNumber, String content, boolean closeHost){
		if(!currentHostHttp.isEmpty() && !currentHostHttp.equals(httpNumber))
			System.out.println("Host uses other http version: " + currentHostHttp);
		
		out.println(command + " " + resource + " " + httpNumber);
		System.out.println(command + " " + resource + " " + httpNumber);
		out.println("Host: " + host);
		System.out.println("Host: " + host);
		out.println("User-Agent: " + "CNHttpChatclient/1.0");
		System.out.println("User-Agent: " + "CNHttpChatclient/1.0");
		if(httpNumber.endsWith("1.1") && closeHost) {			
			out.println("Connection: close");
			System.out.println("Connection: close");
		}
		if(command.toLowerCase().equals("put") || command.toLowerCase().equals("post")) {
			out.println("Content-Type: text/plain");
			System.out.println("Content-Type: text/plain");
			out.println("Content-Length: " + content.length());
			System.out.println("Content-Length: " + content.length());
			out.println();
			System.out.println();
			out.println(content);
			System.out.println(content);
		}
		out.println();
		
		System.out.println();
	}

	public boolean waitForResource(String filename){
		try{
			String line = readLine();
			System.out.println(line + " for resource: " + filename);
			System.out.println();
			if(line.toLowerCase().startsWith("HTTP/"))
				currentHostHttp = line.split(" ")[0].trim();
			if(line.toLowerCase().contains("200 ok")) {
				File file = new File(filename);
				if(filename.contains("/"))
					file.getParentFile().mkdirs();
				file.createNewFile();
				FileOutputStream binWriter = new FileOutputStream(file);
				PrintWriter writer = new PrintWriter(file, "UTF-8");
				
				StringBuffer headers = new StringBuffer();
				boolean binaryContent = false;
				int contentLength = -1;
				while(!line.isEmpty()){
					headers.append(line + "\n");
					if(line.toLowerCase().contains("content-type") && line.toLowerCase().contains("image"))
						binaryContent = true;
					if(line.toLowerCase().contains("content-length")) {
						contentLength = Integer.parseInt(line.split(":")[1].trim());
					}
					line = readLine();
				}
				
				logWriter.write(headers.toString() + "\n");
				byte[] buffer = new byte[contentLength];
				int read = 0;
				int toRead = contentLength;
				do {
					read = in.read(buffer, 0, toRead);
					if(read == -1) break;
					if(binaryContent) binWriter.write(buffer, 0, read);
					else writer.write(new String(buffer, 0, read));
					toRead -= read;
				}while(toRead > 0);
				writer.close();
				binWriter.close();
				return false;
			}
		}catch (IOException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean waitForResponse(){
		String line = readLine();
		if(line.toLowerCase().startsWith("HTTP/")) {
			currentHostHttp = line.split(" ")[0].trim();
			while(!line.isEmpty()){
				System.out.println(line);
				line = readLine();
			}
			return false;
		}
		return true;
	}

	private String readLine(){
		ByteOutputStream bo = new ByteOutputStream();
		try{
			int character;
			do{
				character = in.read();
				if(character == -1)
					break;
				else if(character == '\r'){
					in.mark(2);
					character = in.read();
					if(character == '\n'){
						break;
					}
					in.reset();
				}else if(character == '\n')
					break;
				else bo.write(character);
			}while(character != -1);
			
		}catch (IOException e){
			e.printStackTrace();
		}
		String res = bo.toString();
		bo.close();
		return res;
	}

	public void close(){
		try{
			socket.close();
			in.close();
			out.close();
			logWriter.close();
		}catch (IOException e){
			e.printStackTrace();
		}
	}
}
