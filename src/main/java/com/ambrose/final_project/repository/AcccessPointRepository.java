package com.ambrose.final_project.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ambrose.final_project.model.AccessPoint;

public interface AcccessPointRepository extends JpaRepository<AccessPoint, UUID> {

}
