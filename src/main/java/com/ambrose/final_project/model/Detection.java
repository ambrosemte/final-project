package com.ambrose.final_project.model;

// Hypothetical Detection class
public class Detection {
    public int x, y, width, height;
    public String label;
    public float confidence;

    public Detection(int x, int y, int width, int height, String label, float confidence) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.confidence = confidence;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getLabel() {
        return label;
    }

    public float getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return "Detection [x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + ", label=" + label
                + ", confidence=" + confidence + "]";
    }
}
