package com.alfahospital.alfa_auth_service.service;

import com.alfahospital.alfa_auth_service.domain.Role;
import com.alfahospital.alfa_auth_service.domain.User;
import com.alfahospital.alfa_auth_service.dto.UserSummary;
import com.alfahospital.alfa_auth_service.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    public List<UserSummary> listarUsuarios() {
        return userRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public UserSummary cambiarRol(Long id, Role rol) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con id: " + id));
        user.setRole(rol);
        return toDTO(userRepository.save(user));
    }

    private UserSummary toDTO(User user) {
        return UserSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }
}