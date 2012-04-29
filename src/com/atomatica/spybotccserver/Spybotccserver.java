package com.atomatica.spybotccserver;

import java.io.*;
import java.net.*;

public class Spybotccserver implements Runnable {
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
                System.out.println("Connection " + counter + " received from: " +
                        clientSocket.getInetAddress().getHostName());
                for (int i = 0; i < maxRequests; i++){
                    if (handlers[i] == null) {
                        (handlers[i] = new RequestHandler(clientSocket, handlers)).start();
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
        System.out.println("This program must be run as a daemon using JSVC.");
    }
}

class RequestHandler implements Runnable {
    private RequestHandler handlers[];
    private Thread clientThread;
    private Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    
    public RequestHandler(Socket clientSocket, RequestHandler handlers[]) {
        this.clientSocket = clientSocket;
        this.handlers = handlers;
        
        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(clientSocket.getInputStream());
            System.out.println("RequestHandler thread handling request from: " +
                    clientSocket.getInetAddress().getHostName());
        }
        
        catch (IOException e) {
            System.err.println("Error getting I/O streams for client connection");
        }
    }
    
    public void start() {
        if (clientThread == null) {
            clientThread = new Thread(this);
            clientThread.setDaemon(true);
            clientThread.start();
        }
    }
    
    public void run() {
        try {
            String messageIn = "";
            String messageOut = "SUCCESS";
            sendMessage(messageOut);

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            
            do {
                try {
                    messageIn = (String)input.readObject();
                    System.out.println("Client> " + messageIn);
                    System.out.print("Server> ");
                    messageOut = reader.readLine();
                    sendMessage(messageOut);
                }

                catch (ClassNotFoundException classNotFoundException) {
                    System.out.println("Unknown object type received");
                }

            } while (!messageIn.equals("TERMINATE"));
        }
        
        // process EOFException when client closes connection 
        catch (EOFException e) {
            System.out.println("Server terminated connection");
        }

        // process problems with I/O
        catch (IOException e) {
            e.printStackTrace();
        }

        finally {
            try {
                output.close();
                input.close();
                clientSocket.close();
                System.out.println("Terminated client connection");
            }
            
            catch(IOException e) {
                System.err.println("Error closing client socket");
            }
        }
    }

    // send message to client
    private void sendMessage(String message) {
        try {
            output.writeObject(message);
            output.flush();
        }

        // process problems sending object
        catch (IOException e) {
        }
    }
}
