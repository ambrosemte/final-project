package com.ambrose.final_project.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ambrose.final_project.model.QRCode;

public interface QRCodeRepository extends JpaRepository<QRCode, UUID> {
    public Optional<QRCode> findByLogId(UUID logId);
}
