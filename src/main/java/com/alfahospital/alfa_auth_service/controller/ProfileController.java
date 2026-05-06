package com.alfahospital.alfa_auth_service.controller;

import com.alfahospital.alfa_auth_service.domain.User;
import com.alfahospital.alfa_auth_service.dto.ProfileResponse;
import com.alfahospital.alfa_auth_service.dto.UpdateProfileRequest;
import com.alfahospital.alfa_auth_service.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(
            @RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(profileService.getProfile(email));
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(
            @RequestHeader("X-User-Email") String email,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(email, request));
    }

    @PutMapping("/foto")
    public ResponseEntity<String> updatePhoto(
            @RequestHeader("X-User-Email") String email,
            @RequestParam("file") MultipartFile file) throws IOException {
        profileService.updateProfilePhoto(email, file);
        return ResponseEntity.ok("Foto de perfil actualizada");
    }

    @GetMapping("/foto")
    public ResponseEntity<byte[]> getPhoto(
            @RequestHeader("X-User-Email") String email) {
        User user = profileService.getUserByEmail(email);

        if (user.getProfilePhoto() == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(user.getProfilePhotoContentType()));

        return new ResponseEntity<>(user.getProfilePhoto(), headers, HttpStatus.OK);
    }
}

