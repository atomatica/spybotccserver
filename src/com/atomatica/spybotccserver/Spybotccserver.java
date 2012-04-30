package com.atomatica.spybotccserver;

import java.io.*;
import java.net.*;
import org.apache.commons.daemon.*;

public class Spybotccserver implements Daemon {
    private int port = 9103;
    private int counter = 1;
    private int maxRequests = 10;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private RequestHandler handlers[];
    
    // entry point using JSVC
    public void init(String args[]) {
        if (args.length == 1) {
            port = Integer.valueOf(args[0]).intValue();
        }
        
        handlers = new RequestHandler[maxRequests];
        
        try {
            serverSocket = new ServerSocket(port, 100);
            System.out.println("Initialized Spybot Command and Control Server on port " + port);
        }
        
        catch (IOException e) {
            System.err.println("Error creating server socket");
            System.exit(1);
        }
    }
    
    public void start() {
        run();
    }
    
    public void run() {
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                String clientName = clientSocket.getInetAddress().getHostName();
                System.out.println("Connection " + counter + " received from: " + clientName);
                for (int i = 0; i < maxRequests; i++){
                    if (handlers[i] == null) {
                        (handlers[i] = new RequestHandler(clientName, clientSocket, handlers)).start();
                        break;
                    }
                }
            }
            
            catch (IOException e) {
                System.err.println("Error accepting connection from: " +
                        clientSocket.getInetAddress().getHostName());
            }
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        }
        
        catch (IOException e) {
            System.err.println("Error closing server socket");
            System.exit(1);
        }
    }
    
    public void destroy() {
        serverSocket = null;
    }
    
    public static void main(String args[]) {
        init(args);
        start();
    }
}

class RequestHandler implements Runnable {
    private Thread thread;
    private String clientName;
    private Socket clientSocket;
    private RequestHandler handlers[];
    private ObjectInputStream input;
    private ObjectOutputStream output;
    
    public RequestHandler(String clientName, Socket clientSocket, RequestHandler handlers[]) {
        this.clientName = clientName;
        this.clientSocket = clientSocket;
        this.handlers = handlers;
        
        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(clientSocket.getInputStream());
            System.out.println("RequestHandler thread handling request from: " + clientName);
        }
        
        catch (IOException e) {
            System.err.println("Error getting I/O streams for client: " + clientName);
        }
    }
    
    public void start() {
        if (thread == null) {
            thread = new Thread(this, clientName);
            thread.setDaemon(true);
            thread.start();
        }
    }
    
    public void run() {
        try {
            String message = "";
            output.writeObject("SUCCESS");
            output.flush();
            
            do {
                try {
                    message = (String)input.readObject();
                    System.out.println("Client " + clientName + "> " + message);
                    
                    for (int i = 0; i < handlers.length; i++) {
                        if (handlers[i] != null & handlers[i] != this) {
                            handlers[i].output.writeObject(message);
                            handlers[i].output.flush();
                        }
                    }
                }

                catch (ClassNotFoundException classNotFoundException) {
                    System.err.println("Unknown object type received");
                }

            } while (!message.equals("TERMINATE"));
        }
        
        catch (EOFException e) {
            System.out.println("Client " + clientName + " closed connection");
        }

        catch (IOException e) {
            System.err.println("Error communicating with client: " + clientName);
        }

        finally {
            try {
                output.close();
                input.close();
                clientSocket.close();
                System.out.println("Terminated connection with client: " + clientName);
            }
            
            catch(IOException e) {
                System.err.println("Error closing socket for client: " + clientName);
            }
            
            finally {
                for (int i = 0; i < handlers.length; i++) {
                    if (handlers[i] == this) {
                        handlers[i] = null;
                    }
                }
            }
        }
    }
    
    public void stop() {
        if ((thread != null) && thread.isAlive()) {
            thread = null;
        }
    }
}
