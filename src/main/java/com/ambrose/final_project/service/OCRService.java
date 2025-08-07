package com.ambrose.final_project.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
public class OCRService {
    private Tesseract tesseract;

    public OCRService(Tesseract tesseract) {
        this.tesseract = tesseract;
    }

    public String processNumberPlateOCR(BufferedImage plateRegion) {
        try {
            BufferedImage cleanedPlate = simplePreprocess(plateRegion);
            ImageIO.write(cleanedPlate, "png", new File("debug_plate1.png"));

            String plateNumber = tesseract.doOCR(cleanedPlate);
            return plateNumber.trim();
        } catch (IOException | TesseractException e) {
            e.printStackTrace();
            return "An error occurred";
        }
    }

    private BufferedImage simplePreprocess(BufferedImage input) {
        // Convert to grayscale using standard method
        BufferedImage gray = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();

        // Upscale
        int scale = 5;
        BufferedImage resized = new BufferedImage(gray.getWidth() * scale, gray.getHeight() * scale,
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(gray, 0, 0, resized.getWidth(), resized.getHeight(), null);
        g2.dispose();

        return resized;
    }

    private BufferedImage preprocessForOCR1(BufferedImage input) {
        // Step 1: Convert to grayscale
        BufferedImage gray = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();

        // Step 2: Resize (2x for better OCR)
        int scale = 2;
        BufferedImage resized = new BufferedImage(gray.getWidth() * scale, gray.getHeight() * scale,
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = resized.createGraphics();
        g2.drawImage(gray, 0, 0, resized.getWidth(), resized.getHeight(), null);
        g2.dispose();

        // Step 3: Apply binary threshold
        Raster srcRaster = resized.getData();
        WritableRaster binRaster = srcRaster.createCompatibleWritableRaster();
        int[] pixel = new int[1];

        for (int y = 0; y < resized.getHeight(); y++) {
            for (int x = 0; x < resized.getWidth(); x++) {
                srcRaster.getPixel(x, y, pixel);
                pixel[0] = (pixel[0] < 128) ? 0 : 255; // Simple threshold
                binRaster.setPixel(x, y, pixel);
            }
        }

        BufferedImage binarized = new BufferedImage(resized.getWidth(), resized.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        binarized.setData(binRaster);

        return binarized;
    }

}
