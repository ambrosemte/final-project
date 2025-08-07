package com.ambrose.final_project.service;

import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;

import com.ambrose.final_project.model.Detection;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

@Service
public class YoloDetectorService {
    private OrtEnvironment env;
    private OrtSession session;
    private static final String[] CLASS_LABELS = {
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
            "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
            "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
            "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
            "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
            "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard",
            "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase",
            "scissors", "teddy bear", "hair drier", "toothbrush"
    };

    public YoloDetectorService() {
        try {
            env = OrtEnvironment.getEnvironment();
            session = env.createSession(getClass().getClassLoader().getResource("models/yolov5s.onnx").getPath(),
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
         if (bi.getType() != BufferedImage.TYPE_3BYTE_BGR) {
        // Convert image to 3-byte BGR
        BufferedImage converted = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        converted.getGraphics().drawImage(bi, 0, 0, null);
        bi = converted;
    }
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
            // YOLOv5 output format is typically: [x, y, w, h, conf, class_probs...]
            float x = pred[0], y = pred[1], w = pred[2], h = pred[3], conf = pred[4];

            // Find the class with maximum probability
            int classId = 0;
            float maxProb = 0;
            for (int i = 5; i < pred.length; i++) {
                if (pred[i] > maxProb) {
                    maxProb = pred[i];
                    classId = i - 5;
                }
            }

            // Combine object confidence and class probability
            float totalConf = conf * maxProb;

            if (totalConf > 0.5) {
                int left = (int) ((x - w / 2) * origW / 640);
                int top = (int) ((y - h / 2) * origH / 640);
                int width = (int) (w * origW / 640);
                int height = (int) (h * origH / 640);
                String label = CLASS_LABELS[classId];
                System.out.println("Predicted classId=" + classId + ", label=" + label + ", conf=" + totalConf);

                results.add(new Detection(left, top, width, height, label, totalConf));
            }
        }
        return results;
    }
}
