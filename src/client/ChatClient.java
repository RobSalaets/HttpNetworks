package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sun.jndi.toolkit.url.Uri;

public class ChatClient{
	
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	public ChatClient() {
		
	}
	
	public void connect(String host, int port) {
		String hostParsed = getHost(host);
		try{
			socket = new Socket(hostParsed, port);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
		}catch (IOException e){
			e.printStackTrace();
		}
	}
	
	/*
	public void post(String command, String host, String uri, String httpNumber) {
		String hostParsed = getHost(host);
		System.out.println(command + " " + uri + " " + httpNumber);
		System.out.println("Host: " + hostParsed);
		out.println(command + " " + uri + " " + httpNumber);
		out.println("Host: " + hostParsed);
		out.println();
	}
	*/
	public void post(String command, String fullUri, String httpNumber) {
		String hostParsed = getHost(fullUri);
		String pathParsed = getPath(fullUri);
		System.out.println(command + " " + pathParsed + " " + httpNumber);
		System.out.println("Host: " + hostParsed);
		out.println(command + " " + pathParsed + " " + httpNumber);
		out.println("Host: " + hostParsed);
		out.println();
	}
	
	public boolean poll(String filename) {
		try{
			String line = in.readLine();
			if(line != null) {
				StringBuilder data = new StringBuilder();
				PrintWriter writer = new PrintWriter(filename, "UTF-8");
				boolean htmlLines = false;
				while(in.ready()) {
					if(line.toLowerCase().contains("!doctype"))
						htmlLines = true;
					if(htmlLines)
						data.append(line).append("\n");
					System.out.println(line);
					line = in.readLine();
				}
				lookupEmbedded(data.toString());
				writer.println(data.toString());
				writer.close();
				return false;
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	// TODO
	private void lookupEmbedded(String data) {
		Document doc = Jsoup.parse(data);
		Elements images = doc.select("img");
		for (Element image: images) {
			// TODO: Save image in system
			String imageString = image.attr("src");
			System.out.println("IMAGE URL: " + imageString);
		}
	}
	
	public static String getHost(String fullUri) {
		try {
			return new URL(fullUri).getHost();
		} catch (MalformedURLException ignore) {
		}
		// TODO: legit?
		if (fullUri.charAt(fullUri.length() - 1) == '/') 
			return fullUri.substring(0, fullUri.length()-1);
		return fullUri;
	}

	public static String getPath(String fullUri) {
		URL pathUrl = null;
		try {
			pathUrl = new URL(fullUri);
			if (pathUrl.getPath().length() > 0) return pathUrl.getPath();
		} catch (MalformedURLException ignore) {
		}
		return "/";
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
