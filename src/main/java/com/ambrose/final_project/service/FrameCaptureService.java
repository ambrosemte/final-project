package com.ambrose.final_project.service;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

@Service
public class FrameCaptureService {
    public BufferedImage captureImage() throws IOException {
        // Replace with webcam capture logic or load from video frame
        // return ImageIO.read(new File("example_vehicle.jpg")); // Sample static image
        InputStream imageStream = getClass().getClassLoader().getResourceAsStream("IMG_3132.jpg");
        if (imageStream == null) {
            throw new FileNotFoundException("Image not found in resources.");
        }
        return ImageIO.read(imageStream);
    }
}
