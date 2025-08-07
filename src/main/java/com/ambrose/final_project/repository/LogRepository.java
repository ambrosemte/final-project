package com.ambrose.final_project.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ambrose.final_project.model.Log;

public interface LogRepository extends JpaRepository<Log, UUID> {

    public Optional<Log> findTopByVehicleIdOrderByCreatedAtDesc(UUID vehicleId);

}
