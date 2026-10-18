package com.ambrose.final_project.controller;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ambrose.final_project.dto.response.AccessPointResponse;
import com.ambrose.final_project.dto.response.LogResponse;
import com.ambrose.final_project.dto.response.VehicleResponse;
import com.ambrose.final_project.model.DetectedVehicle;
import com.ambrose.final_project.model.Vehicle;
import com.ambrose.final_project.service.CameraService;
import com.ambrose.final_project.service.FrameCaptureService;
import com.ambrose.final_project.service.VehicleService;

import jakarta.servlet.http.HttpServletResponse;

import org.opencv.core.Mat;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private VehicleService vehicleService;
    private CameraService cameraService;
    private final AtomicInteger activeStreams = new AtomicInteger();

    public VehicleController(VehicleService vehicleService, CameraService cameraService) {
        this.vehicleService = vehicleService;
        this.cameraService = cameraService;
    }

    @GetMapping(value = "/entry-scan", produces = "multipart/x-mixed-replace; boundary=frame")
    public void entry(HttpServletResponse response) throws IOException {
        streamAndProcess(response, "ENTRY");
    }

    @GetMapping(value = "/exit-scan", produces = "multipart/x-mixed-replace; boundary=frame")
    public void exit(HttpServletResponse response) throws IOException {
        streamAndProcess(response, "EXIT");
    }

    private void streamAndProcess(HttpServletResponse response, String actionType) throws IOException {
        if (!cameraService.open()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Camera not available");
            return;
        }
        activeStreams.incrementAndGet();
        response.setContentType("multipart/x-mixed-replace; boundary=frame");

        try {
            OutputStream out = response.getOutputStream();
            while (true) {
                BufferedImage img = cameraService.getFrameAsBufferedImage();
                if (img == null) {
                    Thread.sleep(100); // camera not ready yet, try again
                    continue;
                }
                vehicleService.processVehicleFrame(img, actionType);

                out.write(("--frame\r\nContent-Type: image/jpeg\r\n\r\n").getBytes());
                ImageIO.write(img, "JPEG", out);
                out.write("\r\n".getBytes());
                out.flush();

                Thread.sleep(33); // adjust frame rate
            }
        } catch (IOException | InterruptedException e) {
            // Browser stopped the stream (Stop button, page closed)
        } finally {
            if (activeStreams.decrementAndGet() == 0) {
                cameraService.release();
            }
        }
    }

    @GetMapping("/entry-last")
    public Map<String, String> getLastEntry() {
        DetectedVehicle lastVehicle = vehicleService.getLastDetectedVehicle();

        Map<String, String> response = new HashMap<>();
        if (lastVehicle != null) {
            response.put("plate", lastVehicle.getPlate());
            response.put("confidence", lastVehicle.getConfidence());
        } else {
            response.put("plate", "");
            response.put("confidence", "");
        }
        return response;
    }

    @GetMapping("")
    public List<VehicleResponse> getVehicles() {
        List<Vehicle> vehicles = vehicleService.findAll();

        return vehicles.stream()
                .map(v -> new VehicleResponse(v.getId(), v.getNumberPlate(), v.getColor(), v.getCreatedAt(), null))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public VehicleResponse getVehicle(@PathVariable UUID id) {
        Vehicle vehicle = vehicleService.findById(id);

        List<LogResponse> logResponses = vehicle.getLogs().stream()
                .map(log -> {
                    List<AccessPointResponse> accessPointResponses = log.getAccessPoint().stream()
                            .map(ap -> new AccessPointResponse(
                                    ap.getId(),
                                    ap.getActionType(),
                                    ap.getLocation(),
                                    ap.getName(),
                                    ap.getCreatedAt()))
                            .toList();

                    return new LogResponse(
                            log.getId(),
                            log.getCreatedAt(),
                            accessPointResponses);
                })
                .toList();

        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getNumberPlate(),
                vehicle.getColor(),
                vehicle.getCreatedAt(),
                logResponses);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable UUID id) {
        vehicleService.deleteById(id);
        return "Vehicle deleted";
    }
}
