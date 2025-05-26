//package com.example.posenoisedetectionbackend.controller;
//
//import com.example.posenoisedetectionbackend.model.DetectionResult;
//import com.example.posenoisedetectionbackend.service.NoiseDetectionService;
//import com.example.posenoisedetectionbackend.service.PeopleDetectionService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.util.concurrent.atomic.AtomicLong;
//
//@RestController
//public class DetectionController {
//
//    @Autowired
//    private PeopleDetectionService peopleDetectionService;
//
//    @Autowired
//    private NoiseDetectionService noiseDetectionService;
//
//    @GetMapping(value = "/detect", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<DetectionResult> getDetection() throws IOException, InterruptedException {
//        DetectionResult result = peopleDetectionService.detectPeople();
//        noiseDetectionService.enhanceDetectionResult(result);
//        return ResponseEntity.ok(result);
//    }
//
//    @GetMapping(value = "/stream", produces = "multipart/x-mixed-replace;boundary=frame")
//    public ResponseEntity<StreamingResponseBody> stream() {
//        StreamingResponseBody stream = outputStream -> {
//            File frameFile = new File("src/main/resources/static/live_output.jpg");
//            while (!Thread.currentThread().isInterrupted()) {
//                try {
//                    if (frameFile.exists()) {
//                        byte[] imageBytes = Files.readAllBytes(frameFile.toPath());
//                        outputStream.write("--frame\r\n".getBytes());
//                        outputStream.write("Content-Type: image/jpeg\r\n".getBytes());
//                        outputStream.write(("Content-Length: " + imageBytes.length + "\r\n").getBytes());
//                        outputStream.write("\r\n".getBytes());
//                        outputStream.write(imageBytes);
//                        outputStream.write("\r\n".getBytes());
//                        outputStream.flush();
//                    } else {
//                        System.err.println("Frame file not found during streaming.");
//                    }
//                    Thread.sleep(33); // ~30 FPS
//                } catch (IOException e) {
//                    System.err.println("Error streaming frame: " + e.getMessage());
//                    break; // Exit on client disconnect
//                } catch (InterruptedException e) {
//                    System.err.println("Streaming interrupted: " + e.getMessage());
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//        };
//        return ResponseEntity.ok()
//                .header("Content-Type", "multipart/x-mixed-replace;boundary=frame")
//                .body(stream);
//    }
//}




package com.example.posenoisedetectionbackend.controller;

import com.example.posenoisedetectionbackend.model.DetectionResult;
import com.example.posenoisedetectionbackend.service.NoiseDetectionService;
import com.example.posenoisedetectionbackend.service.PeopleDetectionService;
import com.example.posenoisedetectionbackend.service.PeopleDetectionServiceNew;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/detection")
public class DetectionController {

//    @Autowired
//    private PeopleDetectionService peopleDetectionService;

    @Autowired
    private PeopleDetectionServiceNew peopleDetectionServiceNew;

    @Autowired
    private NoiseDetectionService noiseDetectionService;

    private final AtomicLong lastFrameTime = new AtomicLong(0);

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // Save the uploaded file to a temporary location
            String tempFileName = "temp_image_" + System.currentTimeMillis() + ".jpg";
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            Path tempPath = tempDir.resolve(tempFileName);

            // Ensure temporary directory exists
            Files.createDirectories(tempDir);

            // Save the uploaded file
            File tempFile = tempPath.toFile();
            file.transferTo(tempFile);

            // Process the image using the detection service
            Map<String, Object> result = peopleDetectionServiceNew.detectPeople(tempPath.toString());

            // Delete the temporary file
            tempFile.delete();

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to process image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("src/main/resources/static/" + filename);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

//    @GetMapping(value = "/detect", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<DetectionResult> getDetection() throws IOException, InterruptedException {
//        DetectionResult result = peopleDetectionService.detectPeople();
//        noiseDetectionService.enhanceDetectionResult(result);
//        return ResponseEntity.ok(result);
//    }
//
//    @GetMapping(value = "/frame", produces = MediaType.IMAGE_JPEG_VALUE)
//    public ResponseEntity<byte[]> getFrame() throws IOException {
//        File frameFile = new File("src/main/resources/static/live_output.jpg");
//        if (frameFile.exists()) {
//            byte[] imageBytes = Files.readAllBytes(frameFile.toPath());
//            return ResponseEntity.ok()
//                    .contentType(MediaType.IMAGE_JPEG)
//                    .body(imageBytes);
//        } else {
//            return ResponseEntity.noContent().build();
//        }
//    }
//
//    @GetMapping(value = "/stream", produces = "multipart/x-mixed-replace;boundary=frame")
//    public ResponseEntity<StreamingResponseBody> stream() {
//        StreamingResponseBody stream = outputStream -> {
//            File frameFile = new File("src/main/resources/static/live_output.jpg");
//            long lastModified = 0;
//            while (!Thread.currentThread().isInterrupted()) {
//                try {
//                    if (frameFile.exists()) {
//                        long currentModified = frameFile.lastModified();
//                        if (currentModified > lastModified) {
//                            lastModified = currentModified;
//                            byte[] imageBytes = Files.readAllBytes(frameFile.toPath());
//                            outputStream.write("--frame\r\n".getBytes());
//                            outputStream.write("Content-Type: image/jpeg\r\n".getBytes());
//                            outputStream.write(("Content-Length: " + imageBytes.length + "\r\n").getBytes());
//                            outputStream.write("\r\n".getBytes());
//                            outputStream.write(imageBytes);
//                            outputStream.write("\r\n".getBytes());
//                            outputStream.flush();
//                        }
//                    } else {
//                        System.err.println("Frame file not found, waiting...");
//                        Thread.sleep(100);
//                        continue;
//                    }
//                    Thread.sleep(66);
//                } catch (IOException e) {
//                    System.err.println("Error streaming frame: " + e.getMessage());
//                    break;
//                } catch (InterruptedException e) {
//                    System.err.println("Streaming interrupted: " + e.getMessage());
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//        };
//        return ResponseEntity.ok()
//                .header("Content-Type", "multipart/x-mixed-replace;boundary=frame")
//                .header("Cache-Control", "no-cache")
//                .header("Connection", "keep-alive")
//                .body(stream);
//    }
//
//    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public ResponseEntity<StreamingResponseBody> events() {
//        StreamingResponseBody stream = outputStream -> {
//            ObjectMapper mapper = new ObjectMapper();
//            while (!Thread.currentThread().isInterrupted()) {
//                try {
//                    DetectionResult result = peopleDetectionService.detectPeople();
//                    noiseDetectionService.enhanceDetectionResult(result);
//                    String data = "data: " + mapper.writeValueAsString(result) + "\n\n";
//                    outputStream.write(data.getBytes());
//                    outputStream.flush();
//                    Thread.sleep(500);
//                } catch (Exception e) {
//                    System.err.println("Error in event stream: " + e.getMessage());
//                    break;
//                }
//            }
//        };
//        return ResponseEntity.ok()
//                .header("Content-Type", "text/event-stream")
//                .header("Cache-Control", "no-cache")
//                .header("Connection", "keep-alive")
//                .body(stream);
//    }
}




//
//package com.example.posenoisedetectionbackend.controller;
//
//import com.example.posenoisedetectionbackend.model.DetectionResult;
//import com.example.posenoisedetectionbackend.service.NoiseDetectionService;
//import com.example.posenoisedetectionbackend.service.PeopleDetectionService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.util.concurrent.atomic.AtomicLong;
//
//@RestController
//public class DetectionController {
//
//    @Autowired
//    private PeopleDetectionService peopleDetectionService;
//
//    @Autowired
//    private NoiseDetectionService noiseDetectionService;
//
//    private final AtomicLong lastFrameTime = new AtomicLong(0);
//
//    @GetMapping(value = "/detect", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<DetectionResult> getDetection() throws IOException, InterruptedException {
//        DetectionResult result = peopleDetectionService.detectPeople();
//        noiseDetectionService.enhanceDetectionResult(result);
//        return ResponseEntity.ok(result);
//    }
//
//    @GetMapping(value = "/stream", produces = "multipart/x-mixed-replace;boundary=frame")
//    public ResponseEntity<StreamingResponseBody> stream() {
//        StreamingResponseBody stream = outputStream -> {
//            File frameFile = new File("src/main/resources/static/live_output.jpg");
//            long lastModified = 0;
//            while (!Thread.currentThread().isInterrupted()) {
//                try {
//                    if (frameFile.exists() && frameFile.lastModified() > lastModified) {
//                        lastModified = frameFile.lastModified();
//                        byte[] imageBytes = Files.readAllBytes(frameFile.toPath());
//                        outputStream.write("--frame\r\n".getBytes());
//                        outputStream.write("Content-Type: image/jpeg\r\n".getBytes());
//                        outputStream.write(("Content-Length: " + imageBytes.length + "\r\n").getBytes());
//                        outputStream.write("\r\n".getBytes());
//                        outputStream.write(imageBytes);
//                        outputStream.write("\r\n".getBytes());
//                        outputStream.flush();
//                    }
//                    Thread.sleep(66);
//                } catch (IOException e) {
//                    System.err.println("Error streaming frame: " + e.getMessage());
//                    break;
//                } catch (InterruptedException e) {
//                    System.err.println("Streaming interrupted: " + e.getMessage());
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//        };
//        return ResponseEntity.ok()
//                .header("Content-Type", "multipart/x-mixed-replace;boundary=frame")
//                .body(stream);
//    }
//
//    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public ResponseEntity<StreamingResponseBody> events() {
//        StreamingResponseBody stream = outputStream -> {
//            ObjectMapper mapper = new ObjectMapper();
//            while (!Thread.currentThread().isInterrupted()) {
//                try {
//                    DetectionResult result = peopleDetectionService.detectPeople();
//                    noiseDetectionService.enhanceDetectionResult(result);
//                    String data = "data: " + mapper.writeValueAsString(result) + "\n\n";
//                    outputStream.write(data.getBytes());
//                    outputStream.flush();
//                    Thread.sleep(500);
//                } catch (Exception e) {
//                    System.err.println("Error in event stream: " + e.getMessage());
//                    break;
//                }
//            }
//        };
//        return ResponseEntity.ok()
//                .header("Content-Type", "text/event-stream")
//                .header("Cache-Control", "no-cache")
//                .header("Connection", "keep-alive")
//                .body(stream);
//    }
//}