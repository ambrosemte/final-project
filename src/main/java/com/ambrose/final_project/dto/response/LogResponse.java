package com.ambrose.final_project.dto.response;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class LogResponse {
    private UUID id;
    private Date createdAt;
    private List<AccessPointResponse> accessPoints;

    public LogResponse(UUID id, Date createdAt, List<AccessPointResponse> accessPoints) {
        this.id = id;
        this.createdAt = createdAt;
        this.accessPoints = accessPoints;
    }

    public UUID getId() {
        return id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public List<AccessPointResponse> getAccessPoints() {
        return accessPoints;
    }

}
