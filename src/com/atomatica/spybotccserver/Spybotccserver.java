package com.atomatica.spybotccserver;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.daemon.*;

public class Spybotccserver implements Daemon {
    protected int port = 9103;
    protected ServerSocket serverSocket;
    protected Socket clientSocket;
    protected ExecutorService requestHandlerPool;
    
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
        
        try {
            serverSocket = new ServerSocket(port, 100);
            System.out.println("Spybot Command and Control Server accepting connections on port " + port);
        }
        
        catch (IOException e) {
            System.err.println("Error creating server socket");
            System.exit(1);
        }
        
        requestHandlerPool = Executors.newCachedThreadPool();
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
                requestHandlerPool.execute(new RequestHandler(clientName, clientSocket));
            }
            
            catch (IOException e) {
                System.err.println("Error accepting connection from: " + clientSocket.getInetAddress().getHostName());
            }
        }
    }

    @Override
    public void stop() {
        for (int i = 0; i < maxRequests; i++){
            handlers[i] = null;
        }
    }

    @Override
    public void destroy() {
        try {
            serverSocket.close();
        }
        
        catch (IOException e) {
            System.err.println("Error closing server socket");
            System.exit(1);
        }
        
        serverSocket = null;
    }
    
    public static void main(String args[]) {
        Spybotccserver spybotccserver = new Spybotccserver();
        spybotccserver.init(args);
        spybotccserver.start();
    }
}

class RequestHandler implements Runnable {
    protected String clientName;
    protected Socket clientSocket;
    protected ObjectInputStream input;
    protected ObjectOutputStream output;
    
    public RequestHandler(String clientName, Socket clientSocket) {
        this.clientName = clientName;
        this.clientSocket = clientSocket;
        
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
                    
                    /*for (int i = 0; i < requestHandlerPool.length; i++) {
                        if (handlers[i] != null & handlers[i] != this) {
                            handlers[i].output.writeObject(message);
                            handlers[i].output.flush();
                        }
                    }*/
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
                /*for (int i = 0; i < handlers.length; i++) {
                    if (handlers[i] == this) {
                        handlers[i] = null;
                    }
                }*/
            }
        }
    }
}
