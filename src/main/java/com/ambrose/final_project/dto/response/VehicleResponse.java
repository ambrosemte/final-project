package com.ambrose.final_project.dto.response;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import com.ambrose.final_project.model.User;

public class VehicleResponse {
    private UUID id;
    private String numberPlate;
    private String color;
    private Date createdAt;
    private User user;
    private List<LogResponse> logs;

    public VehicleResponse(UUID id, String numberPlate, String color, Date createdAt, List<LogResponse> logs) {
        this.id = id;
        this.numberPlate = numberPlate;
        this.color = color;
        this.createdAt = createdAt;
        this.logs = logs;
    }

    public UUID getId() {
        return id;
    }

    public String getNumberPlate() {
        return numberPlate;
    }

    public String getColor() {
        return color;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public User getUser() {
        return user;
    }

    public List<LogResponse> getLogs() {
        return logs;
    }

}
