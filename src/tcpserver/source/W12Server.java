package tcpserver.source;

import java.awt.EventQueue;
import java.io.*;
import java.net.*;

import tcpserver.views.Main;

public class W12Server implements Runnable {
	private Main activity;
	String clientSentence;
	String capitalizedSentence;
	ServerSocket welcomeSocket;
	boolean running = false;

	public W12Server(Main parent) {
		activity = parent;
	}
	
	public void run() {
        running = true;
        try {
			welcomeSocket = new ServerSocket(55556);
		} catch (IOException e1) {
			e1.printStackTrace();
			running = false;
		}

        while(running) {
        	Socket connectionSocket = null;
        	try {
        		connectionSocket = welcomeSocket.accept();
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        	BufferedReader inFromClient = null;
        	try {
        		inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        	} catch (IOException e1) {
        		e1.printStackTrace();
        	}
        	DataOutputStream outToClient = null;
        	try {
        		outToClient = new DataOutputStream(connectionSocket.getOutputStream());
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        	try {
        		clientSentence = inFromClient.readLine();
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        	postMessage(clientSentence);
        	System.out.println("Received: " + clientSentence);
        }
		try {
			welcomeSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		running = false;
	}
	
	private void postMessage(final String message) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				activity.receiveMessage(message);
			}
		});
    }
}