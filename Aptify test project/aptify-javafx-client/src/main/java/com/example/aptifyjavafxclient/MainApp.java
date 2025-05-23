package com.example.aptifyjavafxclient;

import com.example.aptifyjavafxclient.model.DetectionResult;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MainApp extends Application {

    private ImageView imageView;
    private Label peopleCountLabel;
    private Label noiseLevelLabel;
    private Label environmentLabel;
    private boolean isBackendRunning = false;

    @Override
    public void start(Stage primaryStage) {
        imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);

        peopleCountLabel = new Label("People Count: 0");
        peopleCountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        noiseLevelLabel = new Label("Noise Level: 0.0000 dB");
        noiseLevelLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        environmentLabel = new Label("Environment: Listening...");
        environmentLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox vbox = new VBox(10, imageView, peopleCountLabel, noiseLevelLabel, environmentLabel);
        vbox.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-background-color: #f8f9fa;");
        Scene scene = new Scene(vbox, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Aptify - Real-Time Detection");
        primaryStage.show();

        checkBackendAvailability();
        startFrameUpdates();
        startDataUpdates();

        primaryStage.setOnCloseRequest(event -> System.exit(0));
    }

    private void checkBackendAvailability() {
        try {
            URL url = new URL("http://localhost:8080/detect");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("Backend is running on localhost:8080");
                isBackendRunning = true;
            } else {
                System.err.println("Backend check failed with code: " + responseCode);
            }
            conn.disconnect();
        } catch (Exception e) {
            System.err.println("Backend is not running: " + e.getMessage());
        }
    }

    private void startFrameUpdates() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isBackendRunning) return;
                try {
                    URL url = new URL("http://localhost:8080/frame");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    if (conn.getResponseCode() == 200) {
                        InputStream stream = conn.getInputStream();
                        Image image = new Image(stream);
                        if (!image.isError()) {
                            Platform.runLater(() -> imageView.setImage(image));
                        } else {
                            System.err.println("Failed to load image: Image is in error state");
                        }
                        stream.close();
                    } else {
                        System.err.println("Frame fetch failed with code: " + conn.getResponseCode());
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    System.err.println("Error fetching frame: " + e.getMessage());
                }
            }
        }, 0, 66); // ~15 FPS
    }

    private void startDataUpdates() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isBackendRunning) return;
                try {
                    URL url = new URL("http://localhost:8080/detect");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        System.out.println("Received JSON: " + response.toString());
                        ObjectMapper mapper = new ObjectMapper();
                        DetectionResult result = mapper.readValue(response.toString(), DetectionResult.class);
                        System.out.println("Deserialized DetectionResult: peopleCount=" + result.getPeopleCount() +
                                ", noiseLevel=" + result.getNoiseLevel() + ", isNoisy=" + result.isNoisy());
                        Platform.runLater(() -> {
                            peopleCountLabel.setText("People Count: " + (result.getPeopleCount() != 0 ? result.getPeopleCount() : 0));
                            noiseLevelLabel.setText("Noise Level: " + String.format("%.4f dB", result.getNoiseLevel() != 0 ? result.getNoiseLevel() : 0.0));
                            environmentLabel.setText("Environment: " + (result.isNoisy() ? "Noisy" : "Quiet"));
                            environmentLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + (result.isNoisy() ? "red" : "green") + ";");
                        });
                    } else {
                        System.err.println("Data fetch failed with code: " + conn.getResponseCode());
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    System.err.println("Error fetching data: " + e.getMessage());
                }
            }
        }, 0, 500); // Update every 0.5 seconds
    }

    public static void main(String[] args) {
        launch(args);
    }
}