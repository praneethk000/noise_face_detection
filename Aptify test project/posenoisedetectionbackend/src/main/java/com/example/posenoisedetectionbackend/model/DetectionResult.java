package com.example.posenoisedetectionbackend.model;

import lombok.Data;

@Data
public class DetectionResult {
    private String peopleImagePath;
    private Integer peopleCount;
    private Double noiseLevel;
    private Boolean isNoisy;
}