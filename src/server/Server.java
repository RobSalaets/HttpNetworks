package server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.sun.media.sound.ModelIdentifier;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

public class Server {
	
	private String command = "";
	private String path = "";
	private String host = "";
	private String httpVersion = "";
	private boolean close = false;
	private boolean disconnected = false;
	private boolean parseError = false;
	private boolean serverError = false;
	private boolean notFound = false;
	private boolean inContent = false;
	private String inputContent;
	private byte[] GETContent;
	private int inContentLength = -1;
	private StringBuilder log = new StringBuilder();

	private boolean containsIfModified = false;
	private boolean isModified = true;
	private String checkModifiedDate;

	
	/*
	 * Read and handle the client input
	 * 
	 * @param	in
	 * 			The BufferedInputStream that is used for reading the input
	 */
	public void processInput(BufferedInputStream in) {
		String inputLine = readLine(in);
		if(inputLine == null) {
			log.append("Client disconnected from server."+ "\n");
			disconnected = true;
			close = true;
			return;
		}
		while(inputLine != null) {
			if(inContent) inputContent += '\n' + inputLine + '\n';
			else parseInput(inputLine);
			
			if(!(inputLine.isEmpty() && !inContent))
				inputLine = readLine(in);
			else return;
		}
	}
	
	/*
	 * This method takes actions on behalf of the given input
	 * 
	 * @param	theInput
	 * 			The input-string that is parsed
	 */
	private void parseInput(String theInput) {
		log.append("---Input: " + theInput+ "\n");
		
		if(theInput.length() >= 3) {
			String linePrefix = theInput.substring(0, 3);
			switch (linePrefix) {
				case "GET":
					command = "GET";
					path = getPath(theInput);
					httpVersion = getHttp(theInput);
					break;
				case "POS":
					command = "POST";
					path = getPath(theInput);
					httpVersion = getHttp(theInput);
					break;
				case "PUT":
					command = "PUT";
					path = getPath(theInput);
					httpVersion = getHttp(theInput);
					break;
				case "HEA":
					command = "HEAD";
					path = getPath(theInput);
					httpVersion = getHttp(theInput);
					break;
				default:
					break;
			}
			
			if (theInput.contains("If-Modified-Since:")) {	// Case sensitive!
				containsIfModified = true;
				checkModifiedDate = theInput.substring(19);
			}
		}
		
		if(theInput.toLowerCase().equals("connection: close")) 
			close = true;
		
		if(theInput.length() > 5 && theInput.toLowerCase().startsWith("host:"))
			host = theInput.split(":")[1].trim();
		
		if(command.equals("PUT") || command.equals("POST")) {
			if(theInput.contains("Content-Length")) {
				String[] a = theInput.split(":");
				
				if(a.length != 2) parseError = true;
				else inContentLength  = Integer.parseInt(a[1].trim());
			}
			if(theInput.isEmpty() && inContentLength > 0) {
				inContent  = true;
				inputContent = "";
			}
		}
	}
	
	/*
	 * Fetch the path out of the given input-line
	 * 
	 * @param	theInput
	 * 			The provided input-line
	 * 
	 * @return	The path
	 */
	private String getPath(String theInput) {
		String[] fields = theInput.split(" ");
		if(fields.length != 3)
			parseError = true;
		else
			return fields[1];
		return "parseError";
	}
	
	/*
	 * Fetch the requested HTTP-code from the input-line
	 * 
	 * @param	theInput
	 * 			The provided input-line
	 * 
	 * @return	The HTTP-number
	 */
	private String getHttp(String theInput) {
		String[] fields = theInput.split(" ");
		if(fields.length != 3)
			parseError = true;
		else
			return fields[2];
		return "parseError";
	}
	
	/*
	 * This method will handle the output corresponding to the collected input-data
	 * 
	 * @return	The output
	 */
	public String getOutput() {
		log.append("---SEND\n");
		StringBuilder headers = new StringBuilder();
		
		// In the case of an invalid input
		if(parseError || (httpVersion.contains("1.1") && host.isEmpty())) {
			appendDefaultHeaders(headers, "404 Bad Request");
		}
		
		// Handle the GET- and HEAD-command
		else if(command.equals("GET") || command.equals("HEAD")) {
			GETContent = getResource(path);
			
			if(GETContent == null) notFound = true;
			
			else {
				if (containsIfModified) {
					try {
						String lastModifiedDate = getFormattedDate();

						SimpleDateFormat dateFormat = new SimpleDateFormat(
								"EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
						Date lastDate = dateFormat.parse(lastModifiedDate);
						Date checkDate = dateFormat.parse(checkModifiedDate);
						
						if (checkDate.before(lastDate)) {
							appendDefaultHeaders(headers, "200 OK");
							String[] ext = path.split("\\.");
							headers.append("Content-Type: " + (path.endsWith("html") || path.endsWith("txt") ? "text/html, charset=UTF-8" : "image/"+ext[ext.length-1]) + "\r\n");
							headers.append("Content-Length: " + GETContent.length+"\r\n");	
						} else {
							headers.append(String.format("HTTP/1.1 " + 304 + " Not Modified" + "\n\r"));
							isModified = false;
							close = true;
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
					
				}
				else {
					appendDefaultHeaders(headers, "200 OK");
					String[] ext = path.split("\\.");
					headers.append("Content-Type: " + (path.endsWith("html") || path.endsWith("txt") ? "text/html, charset=UTF-8" : "image/"+ext[ext.length-1]) + "\r\n");
					headers.append("Content-Length: " + GETContent.length+"\r\n");					
				}
			}
		}
		
		// Handle the PUT-command
		else if(command.equals("PUT")) {
			File file = new File("."+(path.startsWith("/") ? path : "/" + path));
			appendDefaultHeaders(headers, file.exists() ? "200 OK" : "201 Created");
			try{
				if(!file.exists()) file.createNewFile();
				
				PrintWriter writer = new PrintWriter(file, "UTF-8");
				writer.write(inputContent);
				writer.close();
			}catch (IOException e){
				serverError = true;
				e.printStackTrace();
			}
		}
		
		// Handle the POST-command
		else if(command.equals("POST")) {
			File file = new File("."+(path.startsWith("/") ? path : "/" + path));
			appendDefaultHeaders(headers, "200 OK");
			try{
				if(!file.exists()) notFound = true;
				else Files.write(file.toPath(), inputContent.getBytes(), StandardOpenOption.APPEND);
			}catch (IOException e){
				serverError = true;
				e.printStackTrace();
			}
		}
		
		if(notFound) {
			headers = new StringBuilder();
			appendDefaultHeaders(headers, "404 Not Found");
		}
		
		if(serverError) {
			headers = new StringBuilder();
			appendDefaultHeaders(headers, "500 Server Error");
		}
		
		if(httpVersion.contains("1.0") || close)
			headers.append("Connection: close\r\n");
		
		log.append(headers.toString() + "\n");
		headers.append("\r\n");
		
		return headers.toString();
	}
	
	/*
	 * Create the default header
	 * 
	 * @param	headers
	 * 			The total header
	 * @param	statusCode
	 * 			The status code that has to be returned within the header
	 */
	private void appendDefaultHeaders(StringBuilder headers, String statusCode) {
		headers.append(String.format("HTTP/1.1 " + statusCode + "\r\n"));
		headers.append("Date: " + getFormattedDate() +"\r\n");
		headers.append("Server: CN bot server\r\n");
	}
	
	/*
	 * Get the date and time in the formatted form: "EEE, dd MMM yyyy HH:mm:ss z"
	 * 
	 * @return	The date and time
	 */
	private String getFormattedDate() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		return dateFormat.format(calendar.getTime());
	}
	
	/*
	 * Fetch a resource within the file-system
	 * 
	 * @param	fileName
	 * 			The name of the file stored on the server
	 * 
	 * @return	The resource expressed in an byte-array
	 */
	private byte[] getResource(String fileName) {
		File file = new File("."+(fileName.startsWith("/") ? fileName : "/" + fileName));
		
		if(file.exists()) {
			try {
				return Files.readAllBytes(file.toPath());
			} catch (IOException e) {
				log.append("ERROR: Could not read the file"+"\n" + e.getMessage() + "\n");
			}
		}else {
			log.append("ERROR: File doesnt exist" + "\n");
		}
		
		return null;
	}

	/*
	 * Check if the server needs to close
	 * 
	 * @return	A boolean describing if the server needs to close
	 */
	public boolean needToClose(){
		return close;
	}
	
	/*
	 * Read a given line (bytes) and convert to string
	 * 
	 * @param	in
	 * 			The BufferedInputStream that is used to read the lines
	 * 
	 * @return	The line that is read
	 */
	private String readLine(BufferedInputStream in){
		if(inContentLength == 0) return null;
		
		ByteOutputStream bo = new ByteOutputStream();
		int character= -1;
		
		try{
			do{
				character = in.read();
				if(inContent)
					inContentLength--;
				
				if(character == -1) break;
				
				else if(character == '\r'){
					in.mark(2);
					if(!inContent)
						character = in.read();
					if(character == '\n'){
						break;
					}
					in.reset();
				}
				else if(character == '\n') break;
				
				else bo.write(character);
			}while(character != -1 && (!inContent || inContentLength > 0));
		}catch (IOException e){
			e.printStackTrace();
		}
		
		String res = bo.toString();
		bo.close();
		return character == -1 ? null : res;
	}

	/*
	 * This method is used within the class ServerMultiThread to print the server.id
	 */
	public void printLog(String name){
		System.out.println(name + ": \n" + log.toString());
		log = new StringBuilder();
	}

	/*
	 * Return the server its connection state
	 */
	public boolean isDisconnected(){
		return disconnected;
	}

	/*
	 * Return the content if necessary
	 * 
	 * @return	The content
	 */
	public byte[] getContent(){
		if(!notFound && command.equals("GET")) return GETContent;
		return null;
	}
	
	/*
	 * Return the boolean that determines if a given file is changed after a certain date
	 */
	public boolean checkModified() {
		return isModified;
	}
}
