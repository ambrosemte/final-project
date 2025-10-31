package com.ambrose.final_project.service;

import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ambrose.final_project.model.User;
import com.ambrose.final_project.repository.UserRepository;

@Service
public class AuthService {
    private UserRepository userRepository;
   // private PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void register(User user) {
        // if (user.getPassword() != null) {
        //     user.setPassword(passwordEncoder.encode(user.getPassword()));
        // }
        userRepository.save(user);
    }
}
