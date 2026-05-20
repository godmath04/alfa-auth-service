package com.alfahospital.alfa_auth_service.controller;

import com.alfahospital.alfa_auth_service.dto.UserProfileResponse;
import com.alfahospital.alfa_auth_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final AuthService authService;

    @GetMapping("/user-by-email")
    public ResponseEntity<UserProfileResponse> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(authService.getUserProfileByEmail(email));
    }

    @GetMapping("/user-by-id")
    public ResponseEntity<UserProfileResponse> getUserById(@RequestParam Long id) {
        return ResponseEntity.ok(authService.getUserProfileById(id));
    }
}

