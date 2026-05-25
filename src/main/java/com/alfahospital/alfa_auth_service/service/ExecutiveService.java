package com.alfahospital.alfa_auth_service.service;

import com.alfahospital.alfa_auth_service.config.RabbitMQConfig;
import com.alfahospital.alfa_auth_service.domain.UserStatus;
import com.alfahospital.alfa_auth_service.domain.Role;
import com.alfahospital.alfa_auth_service.domain.User;
import com.alfahospital.alfa_auth_service.dto.*;
import com.alfahospital.alfa_auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExecutiveService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final RabbitTemplate    rabbitTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.activation.url:http://localhost:4200/auth/activate-account}")
    private String activationUrl;

    @Value("${app.portal.url:http://localhost:4200}")
    private String portalUrl;

    @Transactional(readOnly = true)
    public List<PatientSearchResponse> searchPatients(String q) {
        return userRepository.searchPacientes(Role.PACIENTE, q)
                .stream().map(this::toSearchResponse).toList();
    }

    @Transactional(readOnly = true)
    public PatientSearchResponse getPatient(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        return toSearchResponse(user);
    }

    @Transactional
    public PatientSearchResponse createGuestPatient(CreateGuestPatientRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Ya existe un usuario con ese email");
        }
        if (userRepository.existsByIdNumber(request.getIdNumber())) {
            throw new RuntimeException("Ya existe un usuario con esa cédula/pasaporte");
        }

        String placeholderPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = User.builder()
                .email(request.getEmail())
                .password(placeholderPassword)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .idType(request.getIdType())
                .idNumber(request.getIdNumber())
                .birthDate(request.getBirthDate())
                .city(request.getCity())
                .gender(request.getGender())
                .role(Role.PACIENTE)
                .status(UserStatus.GUEST)
                .build();

        user = userRepository.save(user);

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("guest-activation:" + token, user.getEmail(), Duration.ofHours(72));

        WelcomeGuestMessage message = WelcomeGuestMessage.builder()
                .email(user.getEmail())
                .nombrePaciente(user.getFirstName() + " " + user.getLastName())
                .portalUrl(activationUrl + "?token=" + token)
                .phone(user.getPhone())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.WELCOME_GUEST_ROUTING_KEY, message);

        return toSearchResponse(user);
    }

    @Transactional
    public AuthResponse activateAccount(ActivateAccountRequest request) {
        String email = redisTemplate.opsForValue().get("guest-activation:" + request.getToken());
        if (email == null) {
            throw new RuntimeException("Token de activación inválido o expirado");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getStatus() != UserStatus.GUEST) {
            throw new RuntimeException("La cuenta ya está activa");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        redisTemplate.delete("guest-activation:" + request.getToken());

        return AuthResponse.builder()
                .email(user.getEmail())
                .role(user.getRole().name())
                .status("ACTIVE")
                .build();
    }

    private PatientSearchResponse toSearchResponse(User u) {
        return PatientSearchResponse.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .idType(u.getIdType())
                .idNumber(u.getIdNumber())
                .status(u.getStatus().name())
                .build();
    }
}
