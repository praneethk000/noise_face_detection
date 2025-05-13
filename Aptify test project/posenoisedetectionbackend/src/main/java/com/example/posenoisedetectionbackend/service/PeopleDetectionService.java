//package com.example.posenoisedetectionbackend.service;
//
//import com.example.posenoisedetectionbackend.model.DetectionResult;
//import org.springframework.stereotype.Service;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStreamReader;
//
//@Service
//public class PeopleDetectionService {
//
//    private static final String PYTHON_SCRIPT_PATH = "people_detection.py"; // Just the filename
//    private static final String OUTPUT_DIR = "src/main/resources/static/";
//    private static final String OUTPUT_FILE = "live_output.jpg";
//    private Process process;
//    private BufferedReader reader;
//    private volatile int peopleCount = 0;
//
//    public PeopleDetectionService() {
//        startPythonScript();
//    }
//
//    private void startPythonScript() {
//        try {
//            ProcessBuilder pb = new ProcessBuilder("python", PYTHON_SCRIPT_PATH, OUTPUT_FILE);
//            pb.directory(new File(OUTPUT_DIR));
//            pb.redirectErrorStream(true);
//            process = pb.start();
//            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//
//            new Thread(() -> {
//                try {
//                    String line;
//                    while ((line = reader.readLine()) != null) {
//                        System.out.println("Python output: " + line);
//                        if (line.startsWith("People detected:")) {
//                            peopleCount = Integer.parseInt(line.split(":")[1].trim());
//                        }
//                    }
//                    System.err.println("Python script stopped unexpectedly.");
//                    restartPythonScript();
//                } catch (IOException e) {
//                    System.err.println("Error reading Python output: " + e.getMessage());
//                }
//            }).start();
//        } catch (IOException e) {
//            System.err.println("Failed to start people detection script: " + e.getMessage());
//        }
//    }
//
//    private void restartPythonScript() {
//        if (process != null) {
//            process.destroy();
//        }
//        startPythonScript();
//    }
//
//    public DetectionResult detectPeople() throws IOException {
//        File outputFile = new File(OUTPUT_DIR + OUTPUT_FILE);
//        if (!outputFile.exists()) {
//            System.err.println("Live output image not found. Checking script status...");
//            if (!process.isAlive()) {
//                restartPythonScript();
//            }
//            throw new IOException("Live output image not found. Script may have failed.");
//        }
//
//        DetectionResult result = new DetectionResult();
//        result.setPeopleImagePath(OUTPUT_FILE);
//        result.setPeopleCount(peopleCount);
//        return result;
//    }
//
//    public void cleanup() {
//        if (process != null) {
//            process.destroy();
//            System.out.println("Python process terminated during cleanup.");
//            process = null;
//        }
//        if (reader != null) {
//            try {
//                reader.close();
//            } catch (IOException e) {
//                System.err.println("Error closing reader: " + e.getMessage());
//            }
//        }
//    }
//}



package com.example.posenoisedetectionbackend.service;

import com.example.posenoisedetectionbackend.model.DetectionResult;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class PeopleDetectionService {

    private static final String PYTHON_SCRIPT_PATH = "people_detection.py";
    private static final String OUTPUT_DIR = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator;
    private static final String OUTPUT_FILE = "live_output.jpg";
    private Process process;
    private BufferedReader reader;
    private volatile int peopleCount = 0;
    private int restartAttempts = 0;
    private static final int MAX_RESTART_ATTEMPTS = 5;

    public PeopleDetectionService() {
        startPythonScript();
    }

    private void startPythonScript() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", PYTHON_SCRIPT_PATH, OUTPUT_FILE);
            pb.directory(new File(OUTPUT_DIR));
            pb.redirectErrorStream(true);
            process = pb.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Python output: " + line);
                        if (line.startsWith("People detected:")) {
                            try {
                                peopleCount = Integer.parseInt(line.split(":")[1].trim());
                            } catch (Exception e) {
                                System.err.println("Error parsing people count: " + line + ", Error: " + e.getMessage());
                            }
                        }
                    }
                    System.err.println("Python script stopped unexpectedly.");
                    if (restartAttempts < MAX_RESTART_ATTEMPTS) {
                        restartPythonScript();
                    } else {
                        System.err.println("Max restart attempts reached for people script.");
                    }
                } catch (IOException e) {
                    System.err.println("Error reading Python output: " + e.getMessage());
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Failed to start people detection script: " + e.getMessage());
        }
    }

    private void restartPythonScript() {
        restartAttempts++;
        if (process != null) {
            process.destroy();
        }
        startPythonScript();
    }

    public DetectionResult detectPeople() throws IOException {
        File outputFile = new File(OUTPUT_DIR + OUTPUT_FILE);
        if (!outputFile.exists()) {
            System.err.println("Live output image not found. Checking script status...");
            if (!process.isAlive()) {
                restartPythonScript();
            }
            throw new IOException("Live output image not found. Script may have failed.");
        }

        DetectionResult result = new DetectionResult();
        result.setPeopleImagePath(OUTPUT_FILE);
        result.setPeopleCount(peopleCount);
        return result;
    }


    public void cleanup() {
        if (process != null) {
            process.destroy();
            System.out.println("Python process terminated during cleanup.");
            process = null;
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                System.err.println("Error closing reader: " + e.getMessage());
            }
        }
    }
}