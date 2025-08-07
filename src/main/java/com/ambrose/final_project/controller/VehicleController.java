package com.ambrose.final_project.controller;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.ambrose.final_project.service.CameraService;
import com.ambrose.final_project.service.VehicleService;

import org.opencv.core.Mat;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private VehicleService vehicleService;
    // private CameraService cameraService;

    // public VehicleController(VehicleService vehicleService, CameraService
    // cameraService) {
    // this.vehicleService = vehicleService;
    // this.cameraService = cameraService;
    // }
    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    // @GetMapping(value = "/entry", produces = MediaType.IMAGE_JPEG_VALUE)
    // public byte[] entry() throws IOException {
    //     vehicleService.processVehicleFrame(cameraService.getFrameAsBufferedImage(), "ENTRY");
    //     return cameraService.getFrameAsBytes();
    // }
    
    @GetMapping(value = "/entry")
    public void entry() {
        vehicleService.processVehicleFrame("ENTRY");
    }

    @GetMapping("/exit")
    public void exit() {
        // vehicleService.processVehicleFrame("EXIT");
    }
}
