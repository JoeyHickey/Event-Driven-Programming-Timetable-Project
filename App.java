package com.mycompany.hellofx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


//23388692_Client

public class App extends Application {
    private static InetAddress host;
    private static final int PORT = 1234;
    private Socket link;
    private BufferedReader in;
    private PrintWriter out;

    //GUI Elements
    private Label responseLabel = new Label("Response From Server Will Display Here");
    private ComboBox<String> dayBox = new ComboBox<>();
    private TextField timeField = new TextField();
    private TextField roomField = new TextField();
    private TextField moduleField = new TextField();
    private ComboBox<String> actionBox = new ComboBox<>();
    private Button sendButton = new Button("Send Request");
    private Button stopButton = new Button("EXIT");
    private Stage schedulePopup = null;


    @Override
    public void start(Stage stage) {
        //initialize dropdown menu
        actionBox.getItems().addAll("Add Lecture", "Remove Lecture", "Display Schedule","Early Lectures", "Other");
        actionBox.setValue("Add Lecture");

        //initialize day menu
        dayBox.getItems().addAll("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
        dayBox.setValue("Monday");

        //set placeholders
        timeField.setPromptText("Enter Time (HH:MM)");
        timeField.addEventFilter(KeyEvent.KEY_TYPED, this::validateTimeInput); //filter to check if only numbers ar entered for time
        roomField.setPromptText("Enter Room Number");
        moduleField.setPromptText("Enter Module Name");

        //set button actions
        sendButton.setOnAction(this::sendRequest);
        stopButton.setOnAction(this::stopConnection);

        //layout
        VBox layout = new VBox(10, actionBox, dayBox, timeField, roomField, moduleField, sendButton, stopButton, responseLabel);
        Scene scene = new Scene(layout, 640, 480);
        stage.setScene(scene);
        stage.setTitle("Lecture Scheduler Client");
        stage.show();

        //establish a connection
        try {
            host = InetAddress.getLocalHost();
            link = new Socket(host, PORT);
            in = new BufferedReader(new InputStreamReader(link.getInputStream()));
            out = new PrintWriter(link.getOutputStream(), true);
        } catch (IOException e) {
            responseLabel.setText("Error: Unable to connect to server.");
        }
    }

    private void sendRequest(ActionEvent event) {
        String action = actionBox.getValue();
        String message = action.toUpperCase() + "|" + dayBox.getValue() + "|" + timeField.getText() + "|" + roomField.getText() + "|" + moduleField.getText();
        out.println(message);

        try {
            String response = in.readLine();
            if (action.equals("Display Schedule")) {
                showSchedulePopup(response);
            } else {
                responseLabel.setText("Server: " + response);
            }


        } catch (IOException e) {
            responseLabel.setText("Error: Communication failure.");
        }
    }

    private void stopConnection(ActionEvent event) {
        try {
            out.println("STOP");
            String response = in.readLine();
            responseLabel.setText("Server: " + response);

            if ("TERMINATE".equals(response)) {
                System.out.println("Closing client application...");
                link.close();
                System.exit(0);
            }
        } catch (IOException e) {
            responseLabel.setText("Error: Unable to close connection.");
        }
    }

    private void validateTimeInput(KeyEvent event) {


        if (!event.getCharacter().matches("[0-9:]")) {
            event.consume();
        }
    }

    private void showSchedulePopup(String scheduleData) {
        if (schedulePopup != null && schedulePopup.isShowing()) {
            schedulePopup.close();
        }

        schedulePopup = new Stage(); // ← reuse the same variable now
        schedulePopup.setTitle("Lecture Schedule");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        String[] hours = {"09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00"};

        for (int i = 0; i < days.length; i++) {
            Label dayLabel = new Label(days[i]);
            grid.add(dayLabel, i + 1, 0);
        }

        for (int i = 0; i < hours.length; i++) {
            Label timeLabel = new Label(hours[i]);
            grid.add(timeLabel, 0, i + 1);
        }

        String[] entries = scheduleData.split(";");
        Map<String, String> slotMap = new HashMap<>();
        for (String entry : entries) {
            if (entry.contains("Day:") && entry.contains("Time:")) {
                String day = entry.split("Day: ")[1].split(" \\|")[0].trim();
                String time = entry.split("Time: ")[1].split(";|\\|")[0].trim();
                slotMap.put(day + " " + time, entry.contains("Module:") ?
                        entry.split("Module: ")[1].split(" \\|")[0] + "\n" + entry.split("Room: ")[1]
                        : "EMPTY");
            }
        }

        for (int row = 0; row < hours.length; row++) {
            for (int col = 0; col < days.length; col++) {
                String key = days[col] + " " + hours[row];
                String content = slotMap.getOrDefault(key, "EMPTY");
                Label cell = new Label(content);
                cell.setStyle("-fx-border-color: black; -fx-padding: 5;");
                grid.add(cell, col + 1, row + 1);
            }
        }

        Scene scene = new Scene(grid, 600, 400);
        schedulePopup.setScene(scene);
        schedulePopup.show();
    }



    public static void main(String[] args) {
        launch();
    }
}