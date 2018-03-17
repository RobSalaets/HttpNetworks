package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Server {
	private static final int WAITING = 0;
	private static final int SEND = 1;

	private int state = WAITING;
	
	private String theOutput;
	private String command;
	private String path;
	private String host;

	public String processInput(String theInput) {
		if (state == WAITING) {
			if (theInput == null)
				System.out.println("---NULL");
				//state = SEND;
			else {
				if (theInput.length() == 0) 
					state = SEND;
				else {
					System.out.println("---Input: " + theInput);
					String firstInput = theInput.substring(0, 3);
					int index;
					switch (firstInput) {
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
						host= theInput.substring(6);
						break;
					default:
						break;
					}
				}
			}
		}

		if (state == SEND) {
			System.out.println("---SEND");
			System.out.println("---Command: " + command);
			System.out.println("---Path: " + path);
			System.out.println("---Host: " + host);
			switch (command) {
			case "GET":
				return getHtml(host, path);
			case "POST":
				break;
			case "PUT":
				break;
			case "HEAD":
				return headHtml(host, path);
			default:
				break;
			}
		}

		return null;
	}
	
	private String getFileAddress(String fileName) {
		return fileName.endsWith("/") ? fileName.substring(0, fileName.length()-1) + ".html" : fileName + ".html";
	}
	
	private String getHeadAddress(String fileName) {
		return fileName.endsWith("/") ? "header_" + fileName.substring(0, fileName.length()-1) + ".txt" : "header_"+fileName+".txt";
	}
	
	private String getHtml(String host, String fileName) {
		File[] files = new File(FileSystems.getDefault().getPath(".").toString()).listFiles();
		fileName = getFileAddress(fileName);
		for (File file: files) {
			if (file.getName().equals(fileName)) {
				try {
					return new String(Files.readAllBytes(Paths.get(fileName)));
				} catch (IOException e) {
					System.out.println("ERROR: Could not read the file");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		
		// TODO: Throw error codes
		return null;
	}
	
	private String headHtml(String host, String fileName) {
		File[] files = new File(FileSystems.getDefault().getPath(".").toString()).listFiles();
		fileName = getHeadAddress(fileName);
		for (File file: files) {
			if (file.getName().equals(fileName)) {
				try {
					return new String(Files.readAllBytes(Paths.get(fileName)));
				} catch (IOException e) {
					System.out.println("ERROR: Could not read the file");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		
		// TODO: Throw error codes
		return null;
	}

}
