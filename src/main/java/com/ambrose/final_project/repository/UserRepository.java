package com.ambrose.final_project.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ambrose.final_project.model.User;

public interface UserRepository extends JpaRepository<User, UUID> {

}
