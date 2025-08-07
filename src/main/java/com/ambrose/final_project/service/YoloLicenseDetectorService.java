package com.ambrose.final_project.service;

import ai.onnxruntime.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.FloatBuffer;
import java.util.*;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import com.ambrose.final_project.model.Detection;

@Service
public class YoloLicenseDetectorService {

    private OrtEnvironment env;
    private OrtSession session;
    private static final String[] CLASS_LABELS = {
            "license_plate"
    };

    public YoloLicenseDetectorService() {
        try {
            env = OrtEnvironment.getEnvironment();
            session = env.createSession(getClass().getClassLoader().getResource("models/lpreditor_anpr.onnx").getPath(),
                    new OrtSession.SessionOptions());
        } catch (OrtException e) {
            e.printStackTrace();
        }
    }

    public List<Detection> detect(BufferedImage image) {
        try {
            float[] inputTensor = preprocessOpenCV(image); // Resize, normalize, etc.
            OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputTensor),
                    new long[] { 1, 3, 640, 640 });

            OrtSession.Result result = session.run(Collections.singletonMap("images", tensor));

            float[][][] output = (float[][][]) result.get(0).getValue(); // adjust depending on model output
            return parseDetections(output[0], image.getWidth(), image.getHeight());

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // private float[] preprocess(BufferedImage img) {
    // // Resize to 640x640 and normalize to 0..1
    // BufferedImage resized = new BufferedImage(640, 640,
    // BufferedImage.TYPE_3BYTE_BGR);
    // resized.getGraphics().drawImage(img, 0, 0, 640, 640, null);

    // float[] data = new float[3 * 640 * 640];
    // int[] rgb = resized.getRGB(0, 0, 640, 640, null, 0, 640);
    // for (int i = 0; i < rgb.length; i++) {
    // int val = rgb[i];
    // int r = (val >> 16) & 0xFF;
    // int g = (val >> 8) & 0xFF;
    // int b = val & 0xFF;

    // int idx = i;
    // data[0 * 640 * 640 + idx] = r / 255.0f;
    // data[1 * 640 * 640 + idx] = g / 255.0f;
    // data[2 * 640 * 640 + idx] = b / 255.0f;
    // }

    // return data;
    // }

    private float[] preprocessOpenCV(BufferedImage img) {
        // Convert BufferedImage to OpenCV Mat
        Mat mat = bufferedImageToMat(img);

        // Resize to 640x640
        Mat resized = new Mat();
        Imgproc.resize(mat, resized, new Size(640, 640));

        // Convert BGR to RGB (YOLO expects RGB input)
        Imgproc.cvtColor(resized, resized, Imgproc.COLOR_BGR2RGB);

        // Normalize to 0..1
        resized.convertTo(resized, CvType.CV_32F, 1.0 / 255.0);

        // HWC to CHW
        List<Mat> channels = new ArrayList<>();
        Core.split(resized, channels);

        float[] data = new float[3 * 640 * 640];
        float[] temp = new float[640 * 640]; // Temporary buffer for each channel

        for (int i = 0; i < 3; i++) {
            channels.get(i).get(0, 0, temp); // Read entire channel into temp
            System.arraycopy(temp, 0, data, i * 640 * 640, 640 * 640); // Copy into main array
        }

        return data;
    }

    private static Mat bufferedImageToMat(BufferedImage bi) {
        int width = bi.getWidth();
        int height = bi.getHeight();
        Mat mat = new Mat(height, width, CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }

    private List<Detection> parseDetections(float[][] outputs, int origW, int origH) {
        List<Detection> results = new ArrayList<>();
        for (float[] pred : outputs) {
            float x = pred[0], y = pred[1], w = pred[2], h = pred[3], conf = pred[4];
            int classId = (int) pred[5];
            if (conf > 0.5) {
                int left = (int) ((x - w / 2) * origW / 640);
                int top = (int) ((y - h / 2) * origH / 640);
                int width = (int) (w * origW / 640);
                int height = (int) (h * origH / 640);
                String label = CLASS_LABELS[classId];
                results.add(new Detection(left, top, width, height, label, conf));
            }
        }
        return results;
    }

    // private List<Detection> parseDetections(float[][] outputs, int origW, int
    // origH) {
    // List<Detection> results = new ArrayList<>();
    // Detection bestDetection = null;
    // float maxConfidence = 0.5f; // Minimum confidence threshold

    // for (float[] pred : outputs) {
    // float x = pred[0], y = pred[1], w = pred[2], h = pred[3], conf = pred[4];
    // int classId = (int) pred[5];

    // if (conf > maxConfidence) {
    // int left = (int) ((x - w / 2) * origW / 640);
    // int top = (int) ((y - h / 2) * origH / 640);
    // int width = (int) (w * origW / 640);
    // int height = (int) (h * origH / 640);

    // // Additional checks for reasonable aspect ratio (typical license plates)
    // float aspectRatio = (float) width / height;
    // if (aspectRatio > 1.5f && aspectRatio < 5.0f) { // Typical plate aspect
    // ratios
    // String label = CLASS_LABELS[classId];
    // Detection detection = new Detection(left, top, width, height, label, conf);

    // // Keep track of the best detection
    // if (bestDetection == null || conf > bestDetection.getConfidence()) {
    // bestDetection = detection;
    // maxConfidence = conf;
    // }
    // }
    // }
    // }

    // if (bestDetection != null) {
    // results.add(bestDetection);
    // }

    // return results;
    // }
}
