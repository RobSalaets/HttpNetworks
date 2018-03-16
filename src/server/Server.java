package server;

public class Server {
    private static final int WAITING = 0;
    private static final int SEND = 1;
    
    private int state = WAITING;
    
    public String processInput(String theInput) {
    	String theOutput = null;
    	String command = null;
    	
    	if (state == WAITING) {
    		if (theInput == null) state = SEND;
    		else if (theInput.substring(0, 3).equals("GET")) command = "GET"; //TODO
    		else if (theInput.substring(0, 3).equals("POS")); // TODO
    		else if (theInput.substring(0, 3).equals("PUT")); // TODO
    		else if (theInput.substring(0, 3).equals("HEA")); // TODO
    		System.out.println("Input: "+theInput);
    		state = SEND;
    	}
    	
    	if (state == SEND) {
    		System.out.println("AAAAAAAA");
    		System.out.println("Command: "+command);
    	}
    	
    	return null;
    }

}
