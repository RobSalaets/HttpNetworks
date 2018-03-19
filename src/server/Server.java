package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class Server {
	
	private String command;
	private String path;
	private String host;
	private boolean close;

	public void processInput(String theInput) {
		System.out.println("---Input: " + theInput);
		if(theInput.length() >= 3) {
			String linePrefix = theInput.substring(0, 3);
			int index;
			switch (linePrefix) {
				case "GET":
					command = "GET";
					index = theInput.indexOf("HTTP");
					path = theInput.substring(4, index - 1);
					break;
				case "POS":
					command = "POST";
					break;
				case "PUT":
					command = "PUT";
					break;
				case "HEA":
					command = "HEAD";
					index = theInput.indexOf("HTTP");
					path = theInput.substring(5, index - 1);
					break;
				case "Hos":
					//TODO check
					host= theInput.substring(6);
					break;
				default:
					break;
			}
			if(theInput.toLowerCase().equals("connection: close"))
				close = true;
			
		}
	}
	
	public String getOutput() {
		System.out.println("---SEND");
		System.out.println("---Command: " + command);
		System.out.println("---Path: " + path);
		System.out.println("---Host: " + host);
		StringBuilder headers = new StringBuilder();
		headers.append("HTTP/1.1 200 OK\r\n");
		headers.append("Date: " + getFormattedDate() +"\r\n");
		if(command.equals("GET") || command.equals("HEAD")) {
			String content = getHtml(path);
			headers.append("Content-Type: text/html, charset=UTF-8\r\n"); //TODO multiple file types
			headers.append("Content-Length: " + content.length()+"\r\n");
			headers.append("\r\n");
			if(command.equals("GET"))
				headers.append(content);
		}
		//TODO PUT POST response
		return headers.toString();
	}
	
	private String getFormattedDate() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z", Locale.FRANCE);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(calendar.getTime());
		
	}
	
	private String getHtml(String fileName) {
		
		File file = new File("./"+fileName);
		if(file.exists()) {
			try {
				return new String(Files.readAllBytes(file.toPath()));
			} catch (IOException e) {
				System.out.println("ERROR: Could not read the file");
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}else {
			System.out.println("ERROR: File doesnt exist");
		}
		
		// TODO: Throw error codes
		return null;
	}

	public boolean needToClose(){
		return close;
	}

}
