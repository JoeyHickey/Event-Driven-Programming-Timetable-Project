package com.mycompany.tcpechoserver;

import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private final ServerGUI gui;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.gui = null; // fallback if GUI isn't passed
    }

    public ClientHandler(Socket socket, ServerGUI gui) {
        this.clientSocket = socket;
        this.gui = gui;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            log("Client connected: " + clientSocket.getInetAddress());

            String request;
            while ((request = in.readLine()) != null) {
                log("Client Request: " + request);

                if (request.equalsIgnoreCase("STOP")) {
                    out.println("TERMINATE");
                    break;
                }

                // EARLY LECTURES gets wrapped in a Task
                if (request.startsWith("EARLY LECTURES")) {
                    Task<Void> earlyLectureTask = new Task<>() {
                        @Override
                        protected Void call() {
                            TCPEchoServer.processEarlyLectures(); // this uses ForkJoinPool internally
                            log("Finished EARLY LECTURES task.");
                            return null;
                        }
                    };

                    new Thread(earlyLectureTask).start();
                    out.println("Shifting Lectures To Earlier Timeslots");
                    log("EARLY LECTURES task started.");
                } else {
                    String response = TCPEchoServer.processRequest(request);
                    out.println(response);
                }
            }
        } catch (IOException e) {
            log("Client communication error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                log("Client disconnected.");
            } catch (IOException e) {
                log("Error closing client socket.");
            }
        }
    }

    private void log(String message) {
        if (gui != null) {
            gui.log(message);
        } else {
            System.out.println(message);
        }
    }
}
