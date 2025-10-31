package com.ambrose.final_project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;

import com.ambrose.final_project.model.AccessPoint;
import com.ambrose.final_project.model.DetectedVehicle;
import com.ambrose.final_project.model.Detection;
import com.ambrose.final_project.model.Log;
import com.ambrose.final_project.model.QRCode;
import com.ambrose.final_project.model.User;
import com.ambrose.final_project.model.Vehicle;
import com.ambrose.final_project.repository.AcccessPointRepository;
import com.ambrose.final_project.repository.LogRepository;
import com.ambrose.final_project.repository.QRCodeRepository;
import com.ambrose.final_project.repository.VehicleRepository;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
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

    private final AtomicReference<DetectedVehicle> lastDetected = new AtomicReference<>();

    // State tracking for vehicle presence
    private Map<String, Boolean> vehiclePresenceState = new HashMap<>();
    private Map<String, Integer> noDetectionFrameCount = new HashMap<>();
    private static final int NO_DETECTION_THRESHOLD = 5; // Reset after 5 consecutive frames with no detection

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

    public void processVehicleFrame(BufferedImage frame, String actionType) {
        try {
            // Step 1: Quick vehicle detection check first
            List<Detection> vehicleDetections = yoloVehicleDetectorService.detect(frame).stream()
                    .filter(d -> d.getLabel().equalsIgnoreCase("car")
                            || d.getLabel().equalsIgnoreCase("person"))
                    .collect(Collectors.toList());

            if (vehicleDetections.isEmpty()) {
                handleNoVehicleDetected(actionType);
                return;
            }

            // Vehicle detected - reset no-detection counter
            noDetectionFrameCount.put(actionType, 0);

            // Check if this is a new vehicle
            boolean wasVehiclePresent = vehiclePresenceState.getOrDefault(actionType, false);

            if (!wasVehiclePresent) {
                // NEW VEHICLE DETECTED - do full processing
                System.out.println("NEW VEHICLE DETECTED for " + actionType);
                processNewVehicle(frame, vehicleDetections, actionType);
                vehiclePresenceState.put(actionType, true);
            } else {
                // Vehicle still present - skip expensive processing
                System.out.println("Vehicle still present for " + actionType + " - skipping processing");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleNoVehicleDetected(String actionType) {
        // System.out.println("No vehicle detected for " + actionType);

        // Increment no-detection counter
        int count = noDetectionFrameCount.getOrDefault(actionType, 0) + 1;
        noDetectionFrameCount.put(actionType, count);

        // Reset state only after several consecutive frames with no detection
        if (count >= NO_DETECTION_THRESHOLD) {
            boolean wasVehiclePresent = vehiclePresenceState.getOrDefault(actionType, false);
            if (wasVehiclePresent) {
                vehiclePresenceState.put(actionType, false);
                System.out.println("Vehicle confirmed left after " + count + " frames - state reset for " + actionType);
            }
        }
    }

    private void processNewVehicle(BufferedImage frame, List<Detection> vehicleDetections, String actionType) {
        try {
            Detection bestVehicleDetection = vehicleDetections.stream()
                    .max(Comparator.comparingDouble(Detection::getConfidence))
                    .orElseThrow(() -> new RuntimeException("No vehicle detection found."));

            BufferedImage vehicle = plateRegionService.extractPlateRegion(frame, bestVehicleDetection);

            // Add timestamp to avoid overwriting debug images
            String timestamp = String.valueOf(System.currentTimeMillis());
            ImageIO.write(vehicle, "png", new File("debug_plate_" + actionType + "_" + timestamp + ".png"));

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

            // Step 4: Extract license plate text
            String numberPlate = ocrService.processNumberPlateOCR(plate);
            System.out.println("Detected License Plate: " + numberPlate);

            // Update last detected vehicle
            DetectedVehicle dv = new DetectedVehicle(numberPlate, String.valueOf(bestPlateDetection.getConfidence()));
            lastDetected.set(dv);

            // Process based on action type
            if (actionType.equals("ENTRY")) {
                initializeVehicleEntryRecord(color, numberPlate);
            } else if (actionType.equals("EXIT")) {
                finalizeVehicleExitRecord(numberPlate);
            }

        } catch (IOException e) {
            System.err.println("Error processing new vehicle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to manually reset state (useful for testing or manual override)
    public void resetVehicleState(String actionType) {
        vehiclePresenceState.put(actionType, false);
        noDetectionFrameCount.put(actionType, 0);
        System.out.println("Manually reset vehicle state for " + actionType);
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

    public DetectedVehicle getLastDetectedVehicle() {
        return lastDetected.get();
    }

    public Vehicle findById(UUID id) {
        Optional<Vehicle> vehicleOptional = vehicleRepository.findById(id);
        if (vehicleOptional.isPresent()) {
            return vehicleOptional.get();
        }
        return null;
    }

    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    public void deleteById(UUID id) {
        vehicleRepository.deleteById(id);
    }
}
