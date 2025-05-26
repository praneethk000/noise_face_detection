package com.example.posenoisedetectionbackend.web_socket_handler;

import com.example.posenoisedetectionbackend.model.DetectionResult;
import com.example.posenoisedetectionbackend.service.PeopleDetectionService;
import com.example.posenoisedetectionbackend.service.PeopleDetectionServiceNew;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


public class DetectionWebSocketHandler extends BinaryWebSocketHandler {
    private final PeopleDetectionServiceNew detectionService;
    private final ObjectMapper objectMapper;

    public DetectionWebSocketHandler() {
        detectionService = new PeopleDetectionServiceNew();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connection established: " + session.getId());
        super.afterConnectionEstablished(session);
    }

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        System.out.println("Received binary message: " + message.getPayloadLength() + " bytes");
        try {
            byte[] imageBytes = message.getPayload().array();
            String tempPath = "src/main/resources/static/temp_image_" + System.currentTimeMillis() + ".jpg";
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            ImageIO.write(image, "jpg", new File(tempPath));

            Map<String, Object> result = detectionService.detectPeople(tempPath);
            new File(tempPath).delete();

            String response = objectMapper.writeValueAsString(result);
            session.sendMessage(new TextMessage(response));
            System.out.println("Sent response: " + response);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error processing image: " + e.getMessage());
            try {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Processing failed: " + e.getMessage());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
            } catch (Exception ex) {
                System.err.println("Error sending error response: " + ex.getMessage());
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket transport error: " + exception.getMessage());
        super.handleTransportError(session, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("WebSocket connection closed: " + session.getId() + ", status: " + status);
        super.afterConnectionClosed(session, status);
    }
}
