package com.atomatica.spybotccserver;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.daemon.*;

public class Spybotccserver implements Daemon {
    protected int port = 9103;
    protected String protocolHeader = "SPYBOTCCP/1.0";
    protected ServerSocket serverSocket;
    protected Socket clientSocket;
    protected ExecutorService requestHandlerPool;
    protected ArrayList<Spybot> spybots;
    
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
        spybots = new ArrayList<Spybot>();
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
        requestHandlerPool.shutdownNow();
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

    class RequestHandler implements Runnable {
        protected String clientName;
        protected Socket clientSocket;
        protected BufferedReader input;
        protected PrintWriter output;
        protected Thread thread;
        
        public RequestHandler(String clientName, Socket clientSocket) {
            this.clientName = clientName;
            this.clientSocket = clientSocket;
            
            try {
                output = new PrintWriter(clientSocket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                System.out.println("RequestHandler thread handling request from: " + clientName);
            }
            
            catch (IOException e) {
                System.err.println("Error getting I/O streams for client: " + clientName);
            }
        }
        
        @Override
        public void run() {
            synchronized (this) {
                thread = Thread.currentThread();
            }
            
            try {
                String message = "";
                output.println(protocolHeader + " " + InetAddress.getLocalHost().getHostName());
                
                while (!thread.isInterrupted()) {
                    message = input.readLine();
                    System.out.println(clientName + " - " + message);
                    
                    // parse message
                    try {
                        String[] tokens = message.split("\\s");
                        
                        if (tokens[0].equals("TERMINATE")) {
                            break;
                        }
                        
                        else if (tokens[0].equals("ANNOUNCE")) {
                            String spybotName = tokens[1];
                            synchronized (spybots) {
                                // TODO: check if spybot name has already been used
                                spybots.add(new Spybot(spybotName, this));
                            }   
                        }
                        
                        else if (tokens[0].equals("CONTROL")) {
                            String spybotName = tokens[1];
                            Spybot spybot = null;
                            synchronized (spybots) {
                                Iterator<Spybot> i = spybots.iterator();
                                while (i.hasNext()) {
                                    Spybot s = i.next();
                                    if (s.name.equals(spybotName)) {
                                        spybot = s;
                                        break;
                                    }
                                }
                                
                                if (spybot == null) {
                                    output.println(protocolHeader + "  300 INVALID");
                                }
                                
                                else {
                                    String passphrase = tokens[2];
                                }
                            }
                        }
                    }
                    
                    catch (ArrayIndexOutOfBoundsException e) {
                        output.println(protocolHeader + "  300 INVALID");
                    }
                }
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
            }
        }
    }
    
    class Spybot {
        protected String name;
        protected RequestHandler requestHandler;
        protected Spybotcontroller controller;
        
        public Spybot(String name, RequestHandler requestHandler) {
            this.name = name;
            this.requestHandler = requestHandler;
            this.controller = null;
        }
        
        public boolean login(String passphrase) {
            requestHandler.output.println("login " + passphrase);
            return false;
        }
    }
    
    class Spybotcontroller {
        
    }
}