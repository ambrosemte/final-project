package com.ambrose.final_project.service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class CameraService {

    // A camera index (0 = first camera, 1 = second, e.g. an iPhone via Continuity Camera)
    // or a stream URL from a phone camera app, e.g. http://192.168.1.20:8080/video
    @Value("${camera.source:0}")
    private String source;

    private VideoCapture capture;
    private Mat frame;

    @PostConstruct
    public void init() {
        nu.pattern.OpenCV.loadShared(); // Load native OpenCV lib
        frame = new Mat(); // must be created after the native lib is loaded
    }

    public synchronized boolean open() {
        if (capture != null && capture.isOpened()) {
            return true;
        }
        capture = source.matches("\\d+")
                ? new VideoCapture(Integer.parseInt(source))
                : new VideoCapture(source);
        if (!capture.isOpened()) {
            System.err.println("Could not open camera source: " + source);
            capture.release();
            capture = null;
            return false;
        }
        return true;
    }

    public synchronized void release() {
        if (capture != null) {
            capture.release();
            capture = null;
        }
    }

    public synchronized byte[] getFrameAsBytes() {
        if (!readFrame()) {
            return null;
        }
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".jpg", frame, buffer);
        return buffer.toArray();
    }

    // Returns null when the camera is not open or no frame is available
    public synchronized BufferedImage getFrameAsBufferedImage() {
        if (!readFrame()) {
            return null;
        }
        return matToBufferedImage(frame);
    }

    private boolean readFrame() {
        return capture != null && capture.isOpened() && capture.read(frame) && !frame.empty();
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

    @PreDestroy
    public void shutdown() {
        release();
    }
}
