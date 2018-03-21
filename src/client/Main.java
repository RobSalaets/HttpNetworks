package client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Main{

	public static void main(String[] args){
		ChatClient client = new ChatClient();
		String[] urlData = getHostAndPath(args[1]);
		String host = urlData[0];
		String resource = urlData[1];
		int port = Integer.parseInt(args[2]);
		String command = args[0];
		String httpNumber = args[3];
		Scanner terminalInput = new Scanner(System.in);

		switch(command){
			case "GET":
				handleGet(client, host, port, command, resource, httpNumber);
				break;
			case "POST":
				System.out.println("Enter content for your " + command + " request: ");
				String contentPost = terminalInput.nextLine();
				handlePutPost(client, host, port, command, resource, httpNumber, contentPost);
				break;
			case "PUT":
				System.out.println("Enter content for your " + command + " request: ");
				String contentPut = terminalInput.nextLine();
				handlePutPost(client, host, port, command, resource, httpNumber, contentPut);
				break;
			case "HEAD":
				// TODO
				handleHead(client, host, port, command, resource, httpNumber);
				break;
			default:
				System.out.println("Command not supported: " + command);
				break;
		}

		terminalInput.close();
		client.close();
	}

	private static void handleGet(ChatClient client, String host, int port, String command, String resource, String httpNumber){
		String htmlFileName = resource.endsWith("html") ? host + resource : resource.endsWith("/") ? host + resource.substring(0, resource.length() - 1) + ".html" : host + resource + ".html";
		System.out.println("fileName: " + htmlFileName);

		try {
			// Request html file
			client.connect(host, port);
			client.httpCommand(command, host, resource, httpNumber, null, false);
		
			while(!client.waitForResource(htmlFileName))
				Thread.sleep(100);
			// Request embedded Images
			Set<String> embeddedImages = lookupEmbedded(htmlFileName);
			List<String> foreignHostResources = new ArrayList<String>();
			int ii = 0;
			for(String imageLocation : embeddedImages){
				String imageResource = imageLocation.startsWith("/") ? imageLocation : "/" + imageLocation;
				String imageHost = host;
				if(imageLocation.contains("//")){
					String[] imageUrlData = getHostAndPath(imageLocation);
					foreignHostResources.add(imageUrlData[0]);
					foreignHostResources.add(imageUrlData[1]);
					continue;
				}
				if(httpNumber.endsWith("1.0"))
					client.connect(imageHost, port);
				client.httpCommand("GET", imageHost, imageResource, httpNumber, null, ii == embeddedImages.size() - 1);
				while(!client.waitForResource("." + imageResource))
					Thread.sleep(100);				
				ii++;
			}
			// Request images from other hosts
			for(int i = 0; i < foreignHostResources.size(); i += 2){
				client.connect(foreignHostResources.get(i), port);
				client.httpCommand("GET", foreignHostResources.get(i), foreignHostResources.get(i + 1), httpNumber, null, i == foreignHostResources.size() - 2);
				while(!client.waitForResource("." + foreignHostResources.get(i + 1)))
					Thread.sleep(100);			
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	private static void handlePutPost(ChatClient client, String host, int port, String command, String resource, String httpNumber, String content){
		try{
			client.connect(host, port);
			client.httpCommand(command, host, resource, httpNumber, content, true);
			while(client.waitForResponse())
				Thread.sleep(100);
		}catch (InterruptedException e){
			e.printStackTrace();
		}
	}
	
	private static void handleHead(ChatClient client, String host, int port, String command, String resource, String httpNumber) {
		client.connect(host, port);
		client.httpCommand(command, host, resource, httpNumber, null, false);
			try {
				while(client.waitForHeader(host))
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	private static Set<String> lookupEmbedded(String fileName){
		String data = "";
		try{
			data = new String(Files.readAllBytes(Paths.get(fileName)));
		}catch (IOException e){
			e.printStackTrace();
		}
		Document doc = Jsoup.parse(data);
		Elements images = doc.select("img[src]");
		Set<String> imageLocations = new HashSet<>();
		for(int i = 0; i < images.size(); i++){
			imageLocations.add(images.get(i).attr("src"));
			System.out.println("IMAGE URL: " + images.get(i).attr("src"));
		}
		System.out.println("");
		return imageLocations;
	}

	public static String[] getHostAndPath(String fullUri){
		String[] result = new String[2];
		if(fullUri.contains("//"))
			try{
				result[0] = new URL(fullUri).getHost();
				String path = new URL(fullUri).getPath();
				if(path.length() > 0)
					result[1] = path;
				else result[1] = "/";
				return result;
			}catch (MalformedURLException e){
				throw new IllegalArgumentException(fullUri);
			}
		else{
			String[] temp = fullUri.split("/", 2);
			if(temp.length == 1 || temp[1].length() == 0){
				result[0] = temp[0];
				result[1] = "/";
				return result;
			} else {
				return fullUri.split("/", 2);
			} 
			/*
			else
				throw new IllegalArgumentException("No host");
				*/
		}
	}
}
