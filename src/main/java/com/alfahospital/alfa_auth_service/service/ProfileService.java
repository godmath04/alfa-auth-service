package com.alfahospital.alfa_auth_service.service;

import com.alfahospital.alfa_auth_service.domain.User;
import com.alfahospital.alfa_auth_service.dto.ProfileResponse;
import com.alfahospital.alfa_auth_service.dto.UpdateProfileRequest;
import com.alfahospital.alfa_auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/png", "image/jpeg", "image/jpg"
    );

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return mapToResponse(user);
    }

    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }

        userRepository.save(user);
        return mapToResponse(user);
    }

    public void updateProfilePhoto(String email, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("El archivo está vacío");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("La imagen no debe superar 2MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new RuntimeException("Solo se permiten imágenes PNG o JPG");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setProfilePhoto(file.getBytes());
        user.setProfilePhotoContentType(file.getContentType());
        userRepository.save(user);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private ProfileResponse mapToResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .idType(user.getIdType())
                .idNumber(user.getIdNumber())
                .birthDate(user.getBirthDate())
                .city(user.getCity())
                .gender(user.getGender())
                .role(user.getRole().name())
                .hasProfilePhoto(user.getProfilePhoto() != null)
                .build();
    }
}

