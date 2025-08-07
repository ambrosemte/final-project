package com.ambrose.final_project.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

@Service
public class QRCodeService {
    private final String charset = "UTF-8";
    private final String stringDateFormat = "yyyyMMddHHmmss";
    private final int width = 200;
    private final int height = 200;

    public String generateQRCode(String data) {
        try {
            String fileName = new SimpleDateFormat(stringDateFormat).format(new Date()) + ".png";
            Path path = Paths.get("src", "main", "resources", "qrcodes", fileName);

            Files.createDirectories(path.getParent());

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    new String(data.getBytes(charset), charset),
                    BarcodeFormat.QR_CODE,
                    width,
                    height);

            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
            System.out.println("QR Code generated at: " + path.toString());
            return path.toString();
        } catch (IOException | WriterException e) {
            e.getStackTrace();
            return "";
        }
    }

    public String readQRCode(String filePath) {
        try {
            // Hardcoded path to a QR code image
            Path path = Paths.get(filePath);

            BufferedImage image = ImageIO.read(path.toFile());
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(bitmap);

            return result.getText();
        } catch (IOException | NotFoundException e) {
            e.printStackTrace();
            return "Failed to scan QR Code.";
        }
    }
}
