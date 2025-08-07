package com.ambrose.final_project.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ambrose.final_project.model.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    public boolean existsByNumberPlate(String numberPlate);

    public Optional<Vehicle> findByNumberPlate(String numberPlate);
}
