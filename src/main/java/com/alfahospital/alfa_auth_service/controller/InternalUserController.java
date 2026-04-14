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
        UserProfileResponse profile = authService.getUserProfileByEmail(email);
        return ResponseEntity.ok(profile);
    }
}

