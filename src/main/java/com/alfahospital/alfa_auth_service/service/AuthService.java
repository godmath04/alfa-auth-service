package com.alfahospital.alfa_auth_service.service;

import com.alfahospital.alfa_auth_service.domain.UserStatus;
import com.alfahospital.alfa_auth_service.domain.Role;
import com.alfahospital.alfa_auth_service.domain.User;
import com.alfahospital.alfa_auth_service.dto.*;

import com.alfahospital.alfa_auth_service.config.RabbitMQConfig;
import com.alfahospital.alfa_auth_service.repository.UserRepository;
import com.alfahospital.alfa_auth_service.util.DocumentoUtils;
import com.alfahospital.alfa_auth_service.util.TelefonoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.reset-password.url:http://localhost:4200/auth/reset-password}")
    private String resetPasswordUrl;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }
        if (userRepository.existsByIdNumber(request.getIdNumber())) {
            throw new RuntimeException("Ya existe un usuario con ese número de identificación");
        }

        boolean documentoValido = "cedula".equals(request.getIdType())
                ? DocumentoUtils.esCedulaValida(request.getIdNumber())
                : DocumentoUtils.esPasaporteValido(request.getIdNumber());
        if (!documentoValido) {
            throw new RuntimeException("Número de identificación inválido");
        }



        // Validar que sea mayor de 18 años
        try {
            java.time.LocalDate birthLocalDate = java.time.LocalDate.parse(request.getBirthDate());
            if (java.time.Period.between(birthLocalDate, java.time.LocalDate.now()).getYears() < 18) {
                throw new RuntimeException("Debes ser mayor de 18 años para registrarte");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Fecha de nacimiento inválida");
        }

        String normalizedPhone = TelefonoUtils.normalizar(request.getPhone());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(normalizedPhone)
                .idType(request.getIdType())
                .idNumber(request.getIdNumber())
                .birthDate(request.getBirthDate())
                .city(request.getCity())
                .gender(request.getGender())
                .role(Role.PACIENTE)
                .status(UserStatus.ACTIVE)
                .build();

        final User savedUser = userRepository.save(user);

        final UserRegisteredMessage message = UserRegisteredMessage.builder()
                .pacienteId(savedUser.getId())
                .pacienteEmail(savedUser.getEmail())
                .idNumber(savedUser.getIdNumber())
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.EXCHANGE,
                            RabbitMQConfig.USER_REGISTERED_ROUTING_KEY,
                            message);
                } catch (Exception e) {
                    // El usuario ya fue creado exitosamente en BD.
                    // Si RabbitMQ falla, logueamos la falla pero NO propagamos la excepción
                    // para que el cliente reciba respuesta de registro exitoso.
                    // Los servicios downstream deberán manejar la ausencia del evento.
                    log.error("[REGISTRO] Fallo al publicar evento user.registered para email={} — RabbitMQ no disponible. Error: {}",
                            savedUser.getEmail(), e.getMessage());
                }
            }
        });

        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole().name(), savedUser.getId());

        return AuthResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (user.getStatus() == UserStatus.GUEST) {
            return AuthResponse.builder().status("GUEST").build();
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new RuntimeException("Cuenta inactiva. Contacte al administrador.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .status("ACTIVE")
                .build();
    }

    public void logout(String token) {
        Date expiration = jwtService.extractExpiration(token);
        long ttl = expiration.getTime() - System.currentTimeMillis();

        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                    "blacklist:" + token,
                    "logout",
                    Duration.ofMillis(ttl)
            );
        }
    }

    public boolean validateToken(String token) {
        Boolean isBlacklisted = redisTemplate.hasKey("blacklist:" + token);
        return jwtService.isTokenValid(token) && Boolean.FALSE.equals(isBlacklisted);
    }

    public UserProfileResponse getUserProfileById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        return UserProfileResponse.builder()
                .id(user.getId())
                .nombre(user.getFirstName())
                .apellido(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .build();
    }

    public UserProfileResponse getUserProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));

        return UserProfileResponse.builder()
                .id(user.getId())
                .nombre(user.getFirstName())
                .apellido(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .build();
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + request.getEmail()));

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("password-reset:" + token, user.getEmail(), Duration.ofHours(1));

        String resetLink = resetPasswordUrl + "?token=" + token;

        PasswordResetMessage message = PasswordResetMessage.builder()
                .email(user.getEmail())
                .resetLink(resetLink)
                .nombrePaciente(user.getFirstName() + " " + user.getLastName())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PASSWORD_RESET_ROUTING_KEY, message);
    }

    public void resetPassword(ResetPasswordRequest request) {
        String email = redisTemplate.opsForValue().get("password-reset:" + request.getToken());

        if (email == null) {
            throw new RuntimeException("Token expirado o inválido");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        redisTemplate.delete("password-reset:" + request.getToken());
    }
}
