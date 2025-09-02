package com.mycompany.tcpechoserver;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerGUI extends Application {

    private static final int PORT = 1234;
    private ServerSocket serverSocket;
    private TextArea logArea;

    @Override
    public void start(Stage primaryStage) {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);

        Button startButton = new Button("Start Server");
        startButton.setOnAction(e -> startServer());

        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> Platform.exit());

        HBox buttonBox = new HBox(10, startButton, exitButton);
        VBox layout = new VBox(10, new Label("TCP Echo Server Log:"), logArea, buttonBox);
        layout.setPadding(new javafx.geometry.Insets(10));

        Scene scene = new Scene(layout, 600, 400);
        primaryStage.setTitle("TCP Echo Server");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void startServer() {
        log("Starting server on port " + PORT + "...");

        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                log("Server started. Waiting for clients...");

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    log("Client connected: " + clientSocket.getInetAddress());

                    Thread clientThread = new Thread(new ClientHandler(clientSocket, this));
                    clientThread.start();
                }

            } catch (IOException e) {
                log("Server error: " + e.getMessage());
                e.printStackTrace();
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}