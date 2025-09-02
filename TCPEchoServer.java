package com.mycompany.tcpechoserver;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class TCPEchoServer {
    private static ServerSocket servSock;
    private static final int PORT = 1234;

    public static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    public static final String[] HOURS = {"09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00"};

    // Shared data structures
    static final Map<String, Lecture> schedule = new HashMap<>();
    static final Set<String> modules = new HashSet<>();

    public static void main(String[] args) {

    }

    // Called from ClientHandler
    public static String processRequest(String request) {
        String[] parts = request.split("\\|");
        if (parts.length < 5) {
            return "ERROR: Invalid request format.";
        }

        String action = parts[0].toUpperCase();
        String day = parts[1];
        String time = parts[2];
        String room = parts[3];
        String module = parts[4];

        switch (action) {
            case "ADD LECTURE":
                return addLecture(day, time, room, module);
            case "REMOVE LECTURE":
                return removeLecture(day, time);
            case "DISPLAY SCHEDULE":
                return displaySchedule();
            case "EARLY LECTURES":
                return processEarlyLectures();

            default:
                return handleInvalidAction(action);

        }
    }

    private static synchronized String addLecture(String day, String time, String room, String module) {
        if (!isValidTime(time)) {
            return "ERROR: Lectures must be on the hour (e.g., 14:00) in 24-hour format.";
        }

        String key = day + " " + time;

        // Synchronize access to the schedule map
        synchronized (schedule) {
            for (Lecture lecture : schedule.values()) {
                if (lecture.day.equals(day) && lecture.time.equals(time) && lecture.room.equals(room)) {
                    return "ERROR: Room " + room + " is already booked at " + time + " on " + day + ".";
                }
            }

            if (!modules.contains(module) && modules.size() >= 5) {
                return "ERROR: Cannot add more than 5 modules.";
            }

            if (schedule.containsKey(key)) {
                return "ERROR: Time slot already booked.";
            }

            // Add the lecture to the schedule and the module to the set
            schedule.put(key, new Lecture(day, time, room, module));
            modules.add(module);
        }

        return "Lecture scheduled: " + module + " on " + day + " at " + time + " in room " + room;
    }

    private static synchronized String removeLecture(String day, String time) {
        String key = day + " " + time;

        synchronized (schedule) {
            if (!schedule.containsKey(key)) {
                return "ERROR: No lecture found at the specified time.";
            }

            Lecture removed = schedule.remove(key);

            // Synchronize access to the modules set
            synchronized (modules) {
                boolean hasMoreLectures = schedule.values().stream().anyMatch(l -> l.module.equals(removed.module));
                if (!hasMoreLectures) {
                    modules.remove(removed.module);
                    return "Lecture removed and module '" + removed.module + "' removed from system.";
                }
            }

            return "Lecture removed from " + day + " at " + time;
        }
    }

    public static String processEarlyLectures() {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        List<RecursiveTask<Boolean>> tasks = new ArrayList<>();

        for (String day : DAYS) {
            List<Lecture> dailyLectures = new ArrayList<>();

            synchronized (schedule) {
                for (Lecture l : schedule.values()) {
                    if (l.day.equals(day)) {
                        dailyLectures.add(l);
                    }
                }
            }

            if (!dailyLectures.isEmpty()) {
                EarlyLecture task = new EarlyLecture(dailyLectures, day);
                tasks.add(task);
                pool.execute(task);
            }
        }

        boolean updated = false;
        for (RecursiveTask<Boolean> task : tasks) {
            updated |= task.join();
        }

        return updated ? "Lectures shifted earlier where possible." : "No changes made.";
    }

    private static synchronized String displaySchedule() {
        if (schedule.isEmpty()) return "No lectures scheduled.";

        StringBuilder sb = new StringBuilder();
        synchronized (schedule) {
            for (String day : DAYS) {
                for (String hour : HOURS) {
                    String key = day + " " + hour;
                    if (schedule.containsKey(key)) {
                        sb.append(schedule.get(key)).append(";");
                    } else {
                        sb.append("EMPTY | Day: ").append(day).append(" | Time: ").append(hour).append(";");
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String handleInvalidAction(String action) {
        try {
            throw new IncorrectActionException("Invalid action: " + action);
        } catch (IncorrectActionException e) {
            return e.getMessage();
        }
    }

    private static boolean isValidTime(String time) {
        return time.matches("([01]?[0-9]|2[0-3]):00");
    }
}




//Lecture class - stores lecture details

class Lecture {
    String day, time, room, module;

    public Lecture(String day, String time, String room, String module) {
        this.day = day;
        this.time = time;
        this.room = room;
        this.module = module;
    }

    @Override
    public String toString() {
        return "Module: " + module + " | Day: " + day + " | Time: " + time + " | Room: " + room;
    }
}


//Exception for Invalid Actions

class IncorrectActionException extends Exception {
    public IncorrectActionException(String message) {
        super(message);
    }
}