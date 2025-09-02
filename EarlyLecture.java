package com.mycompany.tcpechoserver;

import java.util.concurrent.RecursiveTask;
import java.util.*;

class EarlyLecture extends RecursiveTask<Boolean> {
    private static final int SEQUENTIAL_THRESHOLD = 3;

    private List<Lecture> lectures;
    private String day;

    public EarlyLecture(List<Lecture> lectures, String day) {
        this.lectures = lectures;
        this.day = day;
    }

    @Override
    protected Boolean compute() {
        if (lectures.size() <= SEQUENTIAL_THRESHOLD) {
            return shiftLectures(lectures, day);
        } else {
            int mid = lectures.size() / 2;
            EarlyLecture left = new EarlyLecture(lectures.subList(0, mid), day);
            EarlyLecture right = new EarlyLecture(lectures.subList(mid, lectures.size()), day);

            left.fork();
            boolean rightResult = right.compute();
            boolean leftResult = left.join();

            return leftResult || rightResult;
        }
    }

    private boolean shiftLectures(List<Lecture> lectures, String day) {
        boolean changed = false;

        for (Lecture lecture : lectures) {
            String oldKey = day + " " + lecture.time;

            // Find the index of the current time
            int currentIndex = Arrays.asList(TCPEchoServer.HOURS).indexOf(lecture.time);

            if (currentIndex <= 0) continue; // already earliest or invalid

            // Try earlier time slots only
            for (int i = 0; i < currentIndex; i++) {
                String earlierTime = TCPEchoServer.HOURS[i];
                String newKey = day + " " + earlierTime;

                synchronized (TCPEchoServer.schedule) {
                    if (!TCPEchoServer.schedule.containsKey(newKey)) {
                        TCPEchoServer.schedule.remove(oldKey);
                        lecture.time = earlierTime;
                        TCPEchoServer.schedule.put(newKey, lecture);
                        changed = true;
                        break;
                    }
                }
            }
        }

        return changed;
    }

}
