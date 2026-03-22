package com.alfahospital.alfa_auth_service.controller;

import com.alfahospital.alfa_auth_service.domain.Role;
import com.alfahospital.alfa_auth_service.dto.UserSummary;
import com.alfahospital.alfa_auth_service.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<UserSummary>> listarUsuarios() {
        return ResponseEntity.ok(adminUserService.listarUsuarios());
    }

    @PatchMapping("/{id}/rol")
    public ResponseEntity<UserSummary> cambiarRol(
            @PathVariable Long id,
            @RequestParam Role rol) {
        return ResponseEntity.ok(adminUserService.cambiarRol(id, rol));
    }
}