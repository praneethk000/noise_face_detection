package com.example.posenoisedetectionbackend.web_socket_handler;

import com.example.posenoisedetectionbackend.model.DetectionResult;
import com.example.posenoisedetectionbackend.service.PeopleDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DetectionWebSocketHandler extends BinaryWebSocketHandler {
    @Autowired
    private PeopleDetectionService peopleDetectionService;

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        try {
            // Save frame to temporary file
            Path tempFile = Paths.get("src/main/resources/static/temp/temp_frame.jpg");
            Files.write(tempFile, message.getPayload().array());

            // Trigger detection
            DetectionResult result = peopleDetectionService.detectPeople();

            // Send result back
            session.sendMessage(new TextMessage("People detected: " + result.getPeopleCount()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
