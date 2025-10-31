package com.ambrose.final_project.dto.response;

import java.util.Date;
import java.util.UUID;

public class AccessPointResponse {
    private UUID id;
    private String actionType;
    private String location;
    private String name;
    private Date createdAt;

    public AccessPointResponse(UUID id, String actionType, String location, String name, Date createdAt) {
        this.id = id;
        this.actionType = actionType;
        this.location = location;
        this.name = name;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getActionType() {
        return actionType;
    }

    public String getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

}
