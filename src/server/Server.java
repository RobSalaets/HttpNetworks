package server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

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
	private boolean content = false;
	private String inputContent;
	private int contentLength = -1;
	private StringBuilder log = new StringBuilder();

	public void processInput(BufferedInputStream in) {
		String inputLine = readLine(in);
		while(inputLine != null) {
			if(content)
				inputContent += inputLine + "\n";
			else
				parseInput(inputLine);
			if(!(inputLine.isEmpty() && !content))
				inputLine = readLine(in);
			else
				return;
		}
		log.append("Client disconnected from server."+ "\n");
		disconnected = true;
		close = true;

	}
	
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
		}
		if(theInput.toLowerCase().equals("connection: close"))
			close = true;
		if(theInput.length() > 5 && theInput.toLowerCase().startsWith("host:"))
			host = theInput.split(":")[1].trim();
		if(command.equals("PUT") || command.equals("POST")) {
			if(theInput.contains("Content-Length")) {
				String[] a = theInput.split(":");
				if(a.length != 2)
					parseError = true;
				else
					contentLength  = Integer.parseInt(a[1].trim());
			}
			if(theInput.isEmpty() && contentLength > 0) {
				content  = true;
				inputContent = "";
			}
		}
	}
	
	private String getPath(String theInput) {
		String[] fields = theInput.split(" ");
		if(fields.length != 3)
			parseError = true;
		else
			return fields[1];
		return "parseError";
	}
	
	private String getHttp(String theInput) {
		String[] fields = theInput.split(" ");
		if(fields.length != 3)
			parseError = true;
		else
			return fields[2];
		return "parseError";
	}
	
	public String getOutput() {
		log.append("---SEND\n");
		StringBuilder headers = new StringBuilder();
		StringBuilder respContent = new StringBuilder();
		if(parseError || (httpVersion.contains("1.1") && host.isEmpty())) {
			appendDefaultHeaders(headers, "404 Bad Request");
		}else if(command.equals("GET") || command.equals("HEAD")) {
			String content = getHtml(path);
			if(content == null) {
				notFound = true;
			}else {
				appendDefaultHeaders(headers, "200 OK");
				headers.append("Content-Type: text/html, charset=UTF-8\r\n");
				headers.append("Content-Length: " + content.length()+"\r\n");
				if(command.equals("GET"))
					respContent.append(content);
			}
		}else if(command.equals("PUT")) {
			File file = new File("."+(path.startsWith("/") ? path : "/" + path));
			appendDefaultHeaders(headers, file.exists() ? "200 OK" : "201 Created");
			try{
				if(!file.exists()) 
					file.createNewFile();
				PrintWriter writer = new PrintWriter(file, "UTF-8");
				writer.write(inputContent);
				writer.close();
			}catch (IOException e){
				serverError = true;
				e.printStackTrace();
			}
		}else if(command.equals("POST")) {
			File file = new File("."+(path.startsWith("/") ? path : "/" + path));
			appendDefaultHeaders(headers, "200 OK");
			try{
				if(!file.exists())
					notFound = true;
				else 
					Files.write(file.toPath(), inputContent.getBytes(), StandardOpenOption.APPEND);
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
			headers.append("Connection: close");
		headers.append("\r\n");
		log.append(headers.toString() + "\n");
		if(command.equals("GET") && noError())
			headers.append(respContent.toString());
		return headers.toString();
	}
	
	private boolean noError() {
		return !(parseError || serverError || notFound);
	}
	
	private void appendDefaultHeaders(StringBuilder headers, String statusCode) {
		headers.append(String.format("HTTP/1.1 " + statusCode + "\r\n"));
		headers.append("Date: " + getFormattedDate() +"\r\n");
		headers.append("Server: CN bot server\r\n");
	}
	
	private String getFormattedDate() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(calendar.getTime());
		
	}
	
	private String getHtml(String fileName) {
		
		File file = new File("."+(fileName.startsWith("/") ? fileName : "/" + fileName));
		if(file.exists()) {
			try {
				return new String(Files.readAllBytes(file.toPath()));
			} catch (IOException e) {
				log.append("ERROR: Could not read the file"+"\n" + e.getMessage() + "\n");
			}
		}else {
			log.append("ERROR: File doesnt exist" + "\n");
		}
		
		return null;
	}

	public boolean needToClose(){
		return close;
	}
	
	private String readLine(BufferedInputStream in){
		if(contentLength == 0) 
			return null;
		
		ByteOutputStream bo = new ByteOutputStream();
		int character= -1;
		try{
			do{
				character = in.read();
				if(content)
					contentLength--;
				
				if(character == -1)
					break;
				else if(character == '\r'){
					in.mark(2);
					if(!content)
						character = in.read();
					if(character == '\n'){
						break;
					}
					in.reset();
				}else if(character == '\n')
					break;
				else bo.write(character);
			}while(character != -1 && (!content || contentLength > 0));
		}catch (IOException e){
			e.printStackTrace();
		}
		String res = bo.toString();
		bo.close();
		return character == -1 ? null : res;
	}

	public void printLog(String name){
		System.out.println(name + ": \n" + log.toString());
		log = new StringBuilder();
	}

	public boolean isDisconnected(){
		return disconnected;
	}
}
