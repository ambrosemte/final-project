package com.ambrose.final_project.service;

import java.awt.image.BufferedImage;

import org.springframework.stereotype.Service;

import com.ambrose.final_project.model.Detection;

@Service
public class PlateRegionService {
    public BufferedImage extractPlateRegion(BufferedImage image, Detection detection) {
        return image.getSubimage(detection.x, detection.y, detection.width, detection.height);
    }
}
