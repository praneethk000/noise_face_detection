package com.example.posenoisedetectionbackend.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class PeopleDetectionServiceNew {

    public Map<String, Object> detectPeople(String inputImagePath) {
        Map<String, Object> result = new HashMap<>();
        String staticDir = "src/main/resources/static";
        String outputImagePath = staticDir + "/live_output.jpg";

        try {
            // Ensure output directory exists
            Path staticPath = Paths.get(staticDir);
            Files.createDirectories(staticPath);
            System.out.println("Output directory ensured: " + staticPath.toAbsolutePath());

            // Verify input file exists
            Path inputPath = Paths.get(inputImagePath);
            if (!Files.exists(inputPath)) {
                System.out.println("Input image does not exist: " + inputImagePath);
                result.put("error", "Input image not found: " + inputImagePath);
                return result;
            }
            System.out.println("Input image: " + inputImagePath);

            // Run Python script
            ProcessBuilder pb = new ProcessBuilder(
                    "python3",
                    "src/main/resources/static/new_people_detection.py",
                    inputImagePath,
                    outputImagePath
            );
            pb.redirectErrorStream(true); // Merge stderr with stdout
            Process process = pb.start();

            // Capture output and errors
            StringBuilder scriptOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    scriptOutput.append(line).append("\n");
                    System.out.println("Script output: " + line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Python script exit code: " + exitCode);

            // Parse output
            String output = scriptOutput.toString();
            if (exitCode == 0 && output.contains("People detected: ")) {
                String[] lines = output.split("\n");
                for (String line : lines) {
                    if (line.startsWith("People detected: ")) {
                        int peopleCount = Integer.parseInt(line.replace("People detected: ", "").trim());
                        result.put("peopleCount", peopleCount);
                        result.put("peopleImagePath", "live_output.jpg");
                        System.out.println("Detection successful: {} people" + peopleCount);
                        return result;
                    }
                }
                result.put("error", "No valid 'People detected' output from script");
            } else {
                System.out.println("Script failed with exit code " + exitCode + " : " + output);
                result.put("error", "Script execution failed: " + output);
            }
        } catch (Exception e) {
            System.out.println("Detection failed: " + e.getMessage());
            result.put("error", "Detection failed: " + e.getMessage());
        }
        return result;
    }
}
