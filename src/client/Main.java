package client;
public class Main{

	public static void main(String[] args){
		String resource = "/";
		ChatClient client = new ChatClient();
		client.connect(args[1], Integer.parseInt(args[2]));
		client.post(args[0], args[1], resource, args[3]);
		while(client.poll(ChatClient.getHost(args[1]) + ".html")) {
			try{
				Thread.sleep(100);
			}catch (InterruptedException e){
				e.printStackTrace();
			}
		}
		client.close();
	}
}
