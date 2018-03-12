package client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

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
		String htmlFileName = args[1].endsWith("html") ? args[1] : args[1] + ".html";
		
		try {
			client.connect(host, port);
			client.httpCommand(command, host, resource, httpNumber);
			while(client.pollForResource(htmlFileName))
				Thread.sleep(100);
			
			String[] embeddedImages = lookupEmbedded(htmlFileName);
			for(String imageLocation : embeddedImages) {
				String imageResource = imageLocation.startsWith("/") ? imageLocation : "/" + imageLocation;
				String imageHost = host;
				if(imageLocation.contains("//")) {
					String[] imageUrlData = getHostAndPath(imageLocation);
					imageHost = imageUrlData[0];
					imageResource = imageUrlData[1];
				}
				client.connect(imageHost, port);
				client.httpCommand("GET", imageHost, imageResource, httpNumber);
				while(client.pollForResource("." + imageResource))
					Thread.sleep(100);
			}
		}catch (InterruptedException e){
			e.printStackTrace();
		}
		client.close();
	}
	
	private static String[] lookupEmbedded(String fileName) {
		String data = "";
		try{
			data = new String(Files.readAllBytes(Paths.get(fileName)));
		}catch (IOException e) {
			e.printStackTrace();
		}
		Document doc = Jsoup.parse(data);
		Elements images = doc.select("img[src]");
		String[] imageLocations = new String[images.size()];
		for (int i = 0; i < images.size(); i++) {
			imageLocations[i] = images.get(i).attr("src");
			System.out.println("IMAGE URL: " + images.get(i).attr("src"));
		}
		return imageLocations;
	}
	
	public static String[] getHostAndPath(String fullUri) {
		String[] result = new String[2];
		if(fullUri.contains("//")) 
			try {
				result[0] = new URL(fullUri).getHost();
				String path = new URL(fullUri).getPath();
				if (path.length() > 0) result[1] = path;
				else result[1] = "/";
				return result;
			} catch (MalformedURLException ignore) {
				throw new IllegalArgumentException(fullUri);
			}
		else {
			if (fullUri.split("/", 2).length == 1){
				result[0] = fullUri;
				result[1] = "/";
				return result;
			} else if(fullUri.startsWith("www")){
				return fullUri.split("/", 2);
			}
			throw new IllegalArgumentException("No host");
		}
	}
}
