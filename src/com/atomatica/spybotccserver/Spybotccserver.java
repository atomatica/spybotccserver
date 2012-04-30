package com.atomatica.spybotccserver;

import java.io.*;
import java.net.*;
import org.apache.commons.daemon.*;

public class Spybotccserver implements Daemon {
    private int port = 9103;
    private int maxRequests = 10;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private RequestHandler handlers[];
    
    // entry point using JSVC
    @Override
    public void init(DaemonContext dc) {
        String args[] = dc.getArguments();
        init(args);
    }
    
    public void init(String args[]) {
        if (args.length == 1) {
            port = Integer.valueOf(args[0]).intValue();
        }
        
        handlers = new RequestHandler[maxRequests];
        
        try {
            serverSocket = new ServerSocket(port, 100);
            System.out.println("Spybot Command and Control Server accepting connections on port " + port);
        }
        
        catch (IOException e) {
            System.err.println("Error creating server socket");
            System.exit(1);
        }
    }

    @Override
    public void start() {
        run();
    }
    
    public void run() {
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                String clientName = clientSocket.getInetAddress().getHostName();
                System.out.println("Connection received from: " + clientName);
                for (int i = 0; i < maxRequests; i++){
                    if (handlers[i] == null) {
                        handlers[i] = new RequestHandler(clientName, clientSocket, handlers);
                        handlers[i].setDaemon(true);
                        handlers[i].start();
                        break;
                    }
                }
            }
            
            catch (IOException e) {
                System.err.println("Error accepting connection from: " + clientSocket.getInetAddress().getHostName());
            }
        }
    }

    @Override
    public void stop() {
        try {
            serverSocket.close();
        }
        
        catch (IOException e) {
            System.err.println("Error closing server socket");
            System.exit(1);
        }
    }

    @Override
    public void destroy() {
        serverSocket = null;
    }
    
    public static void main(String args[]) {
        Spybotccserver spybotccserver = new Spybotccserver();
        spybotccserver.init(args);
        spybotccserver.start();
    }
}

class RequestHandler extends Thread {
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
    
    @Override
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
                    System.err.println("Unknown object type received from client: " + clientName);
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
                System.out.println("Closed socket with client: " + clientName);
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
}
