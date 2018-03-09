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
	
	public void post(String command, String host, String uri, String httpNumber) {
		String hostParsed = getHost(host);
		System.out.println(command + " " + uri + " " + httpNumber);
		System.out.println("Host: " + hostParsed);
		out.println(command + " " + uri + " " + httpNumber);
		out.println("Host: " + hostParsed);
		out.println();
	}
	
	public boolean poll(String filename) {
		try{
			String line = in.readLine();
			if(line != null) {
				String data = "";
				PrintWriter writer = new PrintWriter(filename, "UTF-8");
				boolean htmlLines = false;
				while(in.ready()) {
					if(line.toLowerCase().contains("!doctype"))
						htmlLines = true;
					if(htmlLines)
						data += line +"\n";
					System.out.println(line);
					line = in.readLine();
				}
				lookupEmbedded(data);
				writer.println(data);
				writer.close();
				return false;
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	private void lookupEmbedded(String data) {
		Document doc = Jsoup.parse(data);
	}
	
	public static String getHost(String host) {
		URL pathUrl = null;
		try {
			pathUrl = new URL(host);
		} catch (MalformedURLException e) {
			System.out.print("Exception catched");
			e.printStackTrace();
		}
		return pathUrl.getHost();
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
