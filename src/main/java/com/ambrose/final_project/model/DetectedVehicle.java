package com.ambrose.final_project.model;

public class DetectedVehicle {
    private String plate;
    private String confidence;

    public DetectedVehicle(String plate, String confidence) {
        this.plate = plate;
        this.confidence = confidence;
    }

    public String getPlate() {
        return plate;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

}
