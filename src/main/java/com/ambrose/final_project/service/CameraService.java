package com.ambrose.final_project.service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class CameraService {

    private VideoCapture capture;
    private Mat frame;

    @PostConstruct
    public void init() {
        nu.pattern.OpenCV.loadShared(); // Load native OpenCV lib
        capture = new VideoCapture(0); // 0 = default camera
        frame = new Mat();
    }

    public byte[] getFrameAsBytes() throws IOException {
        if (capture.isOpened()) {
            capture.read(frame);
            if (!frame.empty()) {
                MatOfByte buffer = new MatOfByte();
                Imgcodecs.imencode(".jpg", frame, buffer);
                return buffer.toArray();
            }
        }
        return null;
    }

    public BufferedImage getFrameAsBufferedImage() {
        Mat frame = new Mat();
        if (capture.isOpened()) {
            capture.read(frame);
        }
        return matToBufferedImage(frame);
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        mat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }
}
