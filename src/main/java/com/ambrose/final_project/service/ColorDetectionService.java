package com.ambrose.final_project.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

@Service
public class ColorDetectionService {

    public String detectDominantColor(BufferedImage image) {
        Mat mat = bufferedImageToMat(image);
        Mat resized = new Mat();
        Imgproc.resize(mat, resized, new Size(1, 1));
        double[] color = resized.get(0, 0);
        return classifyColor(color[2], color[1], color[0]); // r, g, b
    }

    public static String classifyColor(double r, double g, double b) {
        if (r > 200 && g < 100 && b < 100)
            return "Red";
        if (r < 100 && g > 200 && b < 100)
            return "Green";
        if (r < 100 && g < 100 && b > 200)
            return "Blue";
        if (r > 200 && g > 200 && b > 200)
            return "White";
        if (r < 50 && g < 50 && b < 50)
            return "Black";
        if (r > 200 && g > 200 && b < 100)
            return "Yellow";
        return "Unknown" + r + " " + g + " " + b;
    }

    private Mat bufferedImageToMat(BufferedImage bi) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            return Imgcodecs.imdecode(new MatOfByte(baos.toByteArray()), Imgcodecs.IMREAD_COLOR);
        } catch (IOException e) {
            throw new RuntimeException("Conversion failed", e);
        }
    }
}
