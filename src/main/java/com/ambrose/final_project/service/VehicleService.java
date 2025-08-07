package com.ambrose.final_project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ambrose.final_project.model.AccessPoint;
import com.ambrose.final_project.model.Detection;
import com.ambrose.final_project.model.Log;
import com.ambrose.final_project.model.QRCode;
import com.ambrose.final_project.model.Vehicle;
import com.ambrose.final_project.repository.AcccessPointRepository;
import com.ambrose.final_project.repository.LogRepository;
import com.ambrose.final_project.repository.QRCodeRepository;
import com.ambrose.final_project.repository.VehicleRepository;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final LogRepository logRepository;
    private final QRCodeRepository qrCodeRepository;
    private final AcccessPointRepository acccessPointRepository;

    private final FrameCaptureService frameCaptureService;
    private final ColorDetectionService colorDetectionService;
    private final YoloLicenseDetectorService yoloLicenseDetectorService;
    private final YoloDetectorService yoloVehicleDetectorService;
    private final PlateRegionService plateRegionService;
    private final OCRService ocrService;
    private final QRCodeService qrCodeService;

    @Autowired
    public VehicleService(
            VehicleRepository vehicleRepository,
            LogRepository logRepository,
            QRCodeRepository qrCodeRepository,
            AcccessPointRepository acccessPointRepository,
            FrameCaptureService frameCaptureService,
            ColorDetectionService colorDetectionService,
            YoloLicenseDetectorService yoloLicenseDetectorService,
            YoloDetectorService yoloVehicleDetectorService,
            PlateRegionService plateRegionService,
            OCRService ocrService,
            QRCodeService qrCodeService) {
        this.vehicleRepository = vehicleRepository;
        this.logRepository = logRepository;
        this.qrCodeRepository = qrCodeRepository;
        this.acccessPointRepository = acccessPointRepository;
        this.frameCaptureService = frameCaptureService;
        this.colorDetectionService = colorDetectionService;
        this.yoloLicenseDetectorService = yoloLicenseDetectorService;
        this.yoloVehicleDetectorService = yoloVehicleDetectorService;
        this.plateRegionService = plateRegionService;
        this.ocrService = ocrService;
        this.qrCodeService = qrCodeService;
    }

    public void processVehicleFrame(String actionType) {
        try {
            // Step1: Detect Vehicle
            BufferedImage frame = frameCaptureService.captureImage();
            List<Detection> vehicleDetections = yoloVehicleDetectorService.detect(frame).stream()
                    .filter(d -> d.getLabel().equalsIgnoreCase("car")
                            || d.getLabel().equalsIgnoreCase("truck"))
                    .collect(Collectors.toList());

            if (vehicleDetections.isEmpty()) {
                System.out.println("No vehicle detected.");
                return;
            }

            Detection bestVehicleDetection = vehicleDetections.stream()
                    .max(Comparator.comparingDouble(Detection::getConfidence))
                    .orElseThrow(() -> new RuntimeException("No vehicle detection found."));

            BufferedImage vehicle = plateRegionService.extractPlateRegion(frame, bestVehicleDetection);
            ImageIO.write(vehicle, "png", new File("debug_plate.png"));

            // Step 2: Detect color
            String color = colorDetectionService.detectDominantColor(vehicle);
            System.out.println("Detected car color: " + color);

            // Step 3: Detect license plate
            List<Detection> plateDetections = yoloLicenseDetectorService.detect(frame);
            if (plateDetections.isEmpty()) {
                System.out.println("No license plate detected.");
                return;
            }

            Detection bestPlateDetection = plateDetections.get(plateDetections.size() - 1);

            BufferedImage plate = plateRegionService.extractPlateRegion(frame, bestPlateDetection);

            // Step 4: Extract license plate
            String numberPlate = ocrService.processNumberPlateOCR(plate);
            System.out.println("Detected License Plate: " + numberPlate);

            if (actionType == "ENTRY") {
                initializeVehicleEntryRecord(color, numberPlate);
            } else {
                finalizeVehicleExitRecord(numberPlate);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeVehicleEntryRecord(String color, String numberPlate) {

        // Create and save the vehicle
        Vehicle vehicle = new Vehicle();
        if (!vehicleRepository.existsByNumberPlate(numberPlate)) {
            vehicle.setColor(color);
            vehicle.setNumberPlate(numberPlate);
            vehicle = vehicleRepository.save(vehicle);
        } else {

            Optional<Vehicle> vehicleOptional = vehicleRepository.findByNumberPlate(numberPlate);
            vehicle = vehicleOptional.get();
        }

        // Create and save the log
        Log log = new Log();
        log.setVehicle(vehicle);
        log = logRepository.save(log);

        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setLog(log);
        accessPoint.setName("Gate 1");
        accessPoint.setLocation("Gate 1");
        accessPoint.setActionType("ENTRY");
        acccessPointRepository.save(accessPoint);

        // Generate QR code image URL
        String qrCodeImageUrl = qrCodeService.generateQRCode(log.getId().toString());

        // Create QR code and set imageUrl
        QRCode qrCode = new QRCode();
        qrCode.setImageUrl(qrCodeImageUrl);
        qrCode.setLog(log);
        qrCode.setIsExpired(false);
        qrCode = qrCodeRepository.save(qrCode); // save with valid imageUrl

        // Update log with access point and qrCode
        log.getAccessPoint().add(accessPoint);
        log.setQrCode(qrCode);
        logRepository.save(log);
    }

    private void finalizeVehicleExitRecord(String numberPlate) {
        // Find Vehicle
        Vehicle vehicle = new Vehicle();
        Optional<Vehicle> vehicleOptional = vehicleRepository.findByNumberPlate(numberPlate);

        if (vehicleOptional.isPresent()) {
            vehicle = vehicleOptional.get();
        } else {
            // DO IF NOT FOUND
        }

        // FIND LOG
        Log log = new Log();
        Optional<Log> logOptional = logRepository.findTopByVehicleIdOrderByCreatedAtDesc(vehicle.getId());

        if (logOptional.isPresent()) {
            log = logOptional.get();
        } else {
            // DO IF NOT FOUND
        }

        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setLog(log);
        accessPoint.setName("Gate 1");
        accessPoint.setLocation("Gate 1");
        accessPoint.setActionType("EXIT");
        acccessPointRepository.save(accessPoint);

        QRCode qrCode = new QRCode();
        Optional<QRCode> qrCodeOptional = qrCodeRepository.findByLogId(log.getId());
        qrCode = qrCodeOptional.get();

        // READ QR CODE
        String qrCodeData = qrCodeService.readQRCode(qrCode.getImageUrl());

        // validate QR Code
        if (!log.getId().equals(UUID.fromString(qrCodeData)) || qrCode.isExpired() == true) {
            // INVALID
            System.out.println("Invalid!!");
            return;
        }

        // Update QR code is active to false and save
        qrCode.setIsExpired(true);
        qrCodeRepository.save(qrCode); // save
    }
}
