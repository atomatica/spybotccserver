package com.atomatica.spybotccserver;

import java.io.*;
import java.net.*;

public class Server {
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private ServerSocket server;
    private Socket connection;
    private int counter = 1;

    public Server() {
    }

    // set up and run server 
    public void runServer() {
        // set up server to receive connections; process connections
        try {
            // Step 1: Create a ServerSocket.
            server = new ServerSocket(9103, 100);

            while (true) {
                try {
                    waitForConnection(); // Step 2: Wait for a connection.
                    getStreams();        // Step 3: Get input & output streams.
                    processConnection(); // Step 4: Process connection.
                }

                // process EOFException when client closes connection 
                catch (EOFException eofException) {
                    System.out.println("Server terminated connection");
                }

                finally {
                    closeConnection();   // Step 5: Close connection.
                    ++counter;
                }
            }
        }

        // process problems with I/O
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    // wait for connection to arrive, then display connection info
    private void waitForConnection() throws IOException {
        System.out.println("Waiting for connection\n");
        connection = server.accept();      
        System.out.println("Connection " + counter + " received from: " +
                connection.getInetAddress().getHostName());
    }

    // get streams to send and receive data
    private void getStreams() throws IOException {
        // set up output stream for objects
        output = new ObjectOutputStream(connection.getOutputStream());
        
        // flush output buffer to send header information
        output.flush();

        // set up input stream for objects
        input = new ObjectInputStream(connection.getInputStream());

        System.out.println("\nGot I/O streams\n");
    }

    // process connection with client
    private void processConnection() throws IOException {
        // send connection successful message to client
        String message = "Connection successful";
        sendData(message);

        do {
            try {
                message = (String)input.readObject();
                System.out.println("\nClient> " + message);
                sendData("LED1");
                
            }

            // catch problems reading from client
            catch (ClassNotFoundException classNotFoundException) {
                System.out.println("\nUnknown object type received");
            }

        } while (!message.equals("TERMINATE"));
    }

    // close streams and socket
    private void closeConnection() {
        System.out.println("\nTerminating connection\n");

        try {
            output.close();
            input.close();
            connection.close();
        }
        
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    // send message to client
    private void sendData(String message) {
        try {
            output.writeObject( message);
            output.flush();
            System.out.println("\nServer> " + message);
        }

        // process problems sending object
        catch (IOException e) {
        }
    }

    public static void main(String args[]) {
        Server server = new Server();
        server.runServer();
    }
}
