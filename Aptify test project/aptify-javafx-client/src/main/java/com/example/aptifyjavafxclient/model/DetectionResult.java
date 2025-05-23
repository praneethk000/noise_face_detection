package com.example.aptifyjavafxclient.model;

public class DetectionResult {
    private String peopleImagePath;
    private int peopleCount;
    private double noiseLevel;
    private boolean isNoisy;

    // Default constructor for Jackson
    public DetectionResult() {}

    // Getters and setters
    public String getPeopleImagePath() { return peopleImagePath; }
    public void setPeopleImagePath(String peopleImagePath) { this.peopleImagePath = peopleImagePath; }
    public int getPeopleCount() { return peopleCount; }
    public void setPeopleCount(int peopleCount) { this.peopleCount = peopleCount; }
    public double getNoiseLevel() { return noiseLevel; }
    public void setNoiseLevel(double noiseLevel) { this.noiseLevel = noiseLevel; }
    public boolean isNoisy() { return isNoisy; }
    public void setIsNoisy(boolean isNoisy) { this.isNoisy = isNoisy; }
}