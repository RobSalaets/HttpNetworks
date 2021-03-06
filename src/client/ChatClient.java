package client;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;


public class ChatClient{

	private Socket socket;
	private BufferedInputStream in;
	private PrintWriter out;
	private File responseLog;
	private PrintWriter logWriter;
	private String currentHostHttp;
	
	private boolean checkIfModified = false;
	private String date = "Wed, 21 Oct 2020 07:28:00 GMT";	// Keep this format!


	/*
	 * Connect to a given server.
	 * 
	 * @param	host
	 * 			The host server with whom you want to connect
	 * @param	port
	 * 			The port on which the connection is served
	 */
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

	/*
	 * This method will send the user client's command to the server
	 * 
	 * @param	command
	 * 			GET, HEAD, PUT, or POST
	 * @param	host
	 * 			The host server
	 * @param	resource
	 * 			The path within the server
	 * @param	httpNumber
	 * 			HTTP/1.0 or HTTP/1.1
	 * @param	content
	 * 			The content the client wants to send to the host, in the case of a put or post
	 * @param	closeHost
	 * 			Determine if the user wants to close its connection with the host
	 */
	public void httpCommand(String command, String host, String resource, String httpNumber, String content, boolean closeHost){		
		if(!currentHostHttp.isEmpty() && !currentHostHttp.equals(httpNumber))
			System.out.println("Host uses other http version: " + currentHostHttp);
		
		out.println(command + " " + resource + " " + httpNumber);
		System.out.println(command + " " + resource + " " + httpNumber);
		out.println("Host: " + host);
		System.out.println("Host: " + host);
		out.println("User-Agent: " + "CNHttpChatclient/1.0");
		System.out.println("User-Agent: " + "CNHttpChatclient/1.0");
		
		// Handle a check-if-modified-since
		if (checkIfModified) {
			out.println("If-Modified-Since: "+date);
			System.out.print("If-Modified-Since: "+date);
		}
		
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
		else if (command.toLowerCase().equals("head")) {
		}
		
		out.println();
		System.out.println();
	}

	/*
	 * Wait for the resources from a html file, i.e. the embedded objects
	 * 
	 * @param	filename
	 * 			The file where the html is saved
	 * @return	Return false when the user has to wait, true when all the resources are fetched
	 */
	public boolean waitForResource(String filename){
		try{
			String line = readLine();
			if(line == null) return true;
			
			if(line.toLowerCase().startsWith("http/"))
				currentHostHttp = line.split(" ")[0].trim();
			
			if(line.toLowerCase().contains("200 ok")) {
				if (!filename.contains("/")) {
					// Create headerLog-file
					try {
						responseLog = new File("./header_"+filename.substring(0, filename.length()-5)+".txt");
						responseLog.createNewFile();
						logWriter = new PrintWriter(responseLog, "UTF-8");
					} catch (IOException e) {
						System.out.println("ERROR: Unable to create header-file");
						System.out.println(e.getMessage());
					}
				}
				
				
				File file = new File(filename);
				if(filename.contains("/")) file.getParentFile().mkdirs();
				file.createNewFile();
				FileOutputStream binWriter = new FileOutputStream(file);
				PrintWriter writer = new PrintWriter(file, "UTF-8");
				
				StringBuffer headers = new StringBuffer();
				boolean binaryContent = false;
				int contentLength = -1;
				
				while(!line.isEmpty()){
					// Do not save the headers of the embedded objects
					if (!filename.contains("/"))
						logWriter.println(line);
					System.out.println(line);
					headers.append(line + "\n");
					if(line.toLowerCase().contains("content-type") && line.toLowerCase().contains("image"))
						binaryContent = true;
					
					if(line.toLowerCase().contains("content-length"))
						contentLength = Integer.parseInt(line.split(":")[1].trim());
					line = readLine();
				}
				System.out.println();
				
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
				logWriter.close();
				return true;
			} else {
				while(line != null){
					System.out.println(line);
					line = readLine();
				}
				System.out.println();
				
				return true;
			}
		}catch (IOException e){
			e.printStackTrace();
		}
		
		return false;
	}
	
	/*
	 * Wait for the response of the server
	 * 
	 * @return	Return true is the client has to wait, false otherwise
	 */
	public boolean waitForResponse(){
		String line = readLine();
		
		if(line != null && line.toLowerCase().startsWith("http/")) {
			currentHostHttp = line.split(" ")[0].trim();
			while(!line.isEmpty()){
				System.out.println(line);
				line = readLine();
			}
			return false;
		}
		return true;
	}

	/*
	 * Read an input line byte for byte
	 * 
	 * @return	Return the character that corresponds with the byte that is read
	 */
	private String readLine(){
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		int character= -1;
		
		String res = null;
		try{
			do{
				socket.setSoTimeout(2000);
				character = in.read();
				
				if(character == -1) break;
				
				else if(character == '\r'){
					in.mark(2);
					character = in.read();
					if(character == '\n') break;
					in.reset();	
				}
				else if(character == '\n') break;
				
				else bo.write(character);
			}while(character != -1);
			res  = bo.toString();
			bo.close();
		}catch (IOException e){
			System.out.println();
			System.out.println(e.getMessage());
		}
		
		
		return character == -1 ? null : res;
	}

	/*
	 * Close the connection with the host
	 */
	public void close(){
		try{
			if (socket != null) socket.close();
			if (in != null) in.close();
			if (out != null) out.close();
			if (logWriter != null) logWriter.close();
		}catch (IOException e){
			e.printStackTrace();
		}
	}
	
	/*
	 * Receive and store the header of the http request
	 * 
	 * @param	filename
	 * 			The file where the html file is stored
	 */
	public boolean waitForHeader(String filename) {		
		String line = readLine();
		if(line.toLowerCase().startsWith("http/"))
			currentHostHttp = line.split(" ")[0].trim();
		
		if(line.toLowerCase().contains("200 ok")) {
			// Create a headerLog-file
			if (!filename.contains("/")) {
				try {
					responseLog = new File("./header_"+filename+".txt");
					responseLog.createNewFile();
					logWriter = new PrintWriter(responseLog, "UTF-8");
				} catch (IOException e) {
					System.out.println("ERROR: Unable to create header-file");
					System.out.println(e.getMessage());
				}
			}
			
			StringBuffer headers = new StringBuffer();
			while(!line.isEmpty()){
				if (!filename.contains("/")) logWriter.println(line);
				headers.append(line + "\n");
				line = readLine();
			}
			logWriter.close();
			System.out.println(headers.toString());
			return false;
		}
		return true;
	}
}
