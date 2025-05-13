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
//public class NoiseDetectionService {
//
//    private static final String PYTHON_SCRIPT_PATH = "noise_detection.py"; // Just filename
//    private static final String OUTPUT_DIR = "src/main/resources/static/";
//    private static final String OUTPUT_FILE = "noise_result.json";
//    private Process process;
//    private BufferedReader reader;
//    private volatile double noiseLevel = 0.0;
//    private volatile boolean isNoisy = false;
//
//    public NoiseDetectionService() {
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
//                        System.out.println("Noise script output: " + line);
//                        if (line.startsWith("Noise RMS:")) {
//                            String[] parts = line.split(",");
//                            String rmsPart = parts[0].replace("Noise RMS:", "").trim();
//                            String noisyPart = parts[1].replace("Is Noisy:", "").trim();
//                            noiseLevel = Double.parseDouble(rmsPart);
//                            isNoisy = Boolean.parseBoolean(noisyPart);
//                        }
//                    }
//                    System.err.println("Noise script stopped unexpectedly.");
//                    restartPythonScript();
//                } catch (IOException e) {
//                    System.err.println("Error reading noise script output: " + e.getMessage());
//                }
//            }).start();
//        } catch (IOException e) {
//            System.err.println("Failed to start noise detection script: " + e.getMessage());
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
//    public void enhanceDetectionResult(DetectionResult result) {
//        result.setNoiseLevel(noiseLevel);
//        result.setIsNoisy(isNoisy);
//    }
//
//    public void cleanup() {
//        if (process != null) {
//            process.destroy();
//            System.out.println("Noise detection process terminated during cleanup.");
//            process = null;
//        }
//        if (reader != null) {
//            try {
//                reader.close();
//            } catch (IOException e) {
//                System.err.println("Error closing noise reader: " + e.getMessage());
//            }
//        }
//    }
//}



package com.example.posenoisedetectionbackend.service;

import com.example.posenoisedetectionbackend.model.DetectionResult;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;

@Service
public class NoiseDetectionService {

    private static final String PYTHON_SCRIPT_PATH = "noise_detection.py";
    private static final String OUTPUT_DIR = "src/main/resources/static/";
    private Process process;
    private BufferedReader reader;
    private volatile double noiseLevel = 0.0;
    private volatile boolean isNoisy = false;

    public NoiseDetectionService() {
        startPythonScript();
    }

    private void startPythonScript() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", PYTHON_SCRIPT_PATH);
            pb.directory(new File(OUTPUT_DIR));
            pb.redirectErrorStream(true);
            process = pb.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Noise script output: " + line);
                        if (line.startsWith("Noise RMS:")) {
                            String[] parts = line.split(",");
                            if (parts.length == 2) {
                                String rmsPart = parts[0].replace("Noise RMS:", "").trim();
                                String noisyPart = parts[1].replace("Is Noisy:", "").trim();
                                try {
                                    double rawRms = Double.parseDouble(rmsPart);
                                    if (!Double.isNaN(rawRms) && !Double.isInfinite(rawRms)) {
                                        noiseLevel = rawRms * 20; // Approximate dB
                                        isNoisy = Boolean.parseBoolean(noisyPart);
                                        System.out.println("Updated: noiseLevel=" + noiseLevel + "dB, isNoisy=" + isNoisy);
                                    } else {
                                        System.err.println("Invalid RMS value: " + rmsPart + ", skipping...");
                                    }
                                } catch (NumberFormatException e) {
                                    System.err.println("Failed to parse RMS: " + rmsPart + ", error: " + e.getMessage());
                                }
                            }
                        }
                    }
                    System.err.println("Noise script stopped unexpectedly. Restarting...");
                    restartPythonScript();
                } catch (IOException e) {
                    System.err.println("Error reading noise script: " + e.getMessage());
                    restartPythonScript();
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Failed to start noise script: " + e.getMessage());
        }
    }

    private void restartPythonScript() {
        if (process != null) {
            process.destroy();
            try { Thread.sleep(1000); } catch (InterruptedException e) { /* Ignore */ }
        }
        startPythonScript();
    }

    public void enhanceDetectionResult(DetectionResult result) {
        result.setNoiseLevel(noiseLevel);
        result.setIsNoisy(isNoisy);
    }

    public DetectionResult getLatestNoiseData() {
        DetectionResult result = new DetectionResult();
        enhanceDetectionResult(result);
        return result;
    }

    public void cleanup() {
        if (process != null) {
            process.destroy();
            System.out.println("Noise process terminated.");
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