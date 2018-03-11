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
	
	public void connect(String fullUri, int port) {
		String hostParsed = getHostAndPath(fullUri)[0];
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
		String[] uriParsed = getHostAndPath(fullUri);
		String hostParsed = uriParsed[0];//getHost(fullUri);
		String pathParsed = uriParsed[1];//getPath(fullUri);
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
	
	public static String[] getHostAndPath(String fullUri) {
		try {
			String[] result = new String[2];
			result[0] = new URL(fullUri).getHost();
			String path = new URL(fullUri).getPath();
			if (path.length() > 0) result[1] = path;
			else result[1] = "/";
			return result;
		} catch (MalformedURLException ignore) {
		}
		return fullUri.split("/", 2);
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
