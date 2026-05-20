package com.alfahospital.alfa_auth_service.controller;

import com.alfahospital.alfa_auth_service.dto.CreateGuestPatientRequest;
import com.alfahospital.alfa_auth_service.dto.PatientSearchResponse;
import com.alfahospital.alfa_auth_service.service.ExecutiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth/ejecutivo")
@RequiredArgsConstructor
public class ExecutiveController {

    private final ExecutiveService executiveService;

    @GetMapping("/pacientes")
    public ResponseEntity<List<PatientSearchResponse>> buscar(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam String q) {
        requireEjecutivo(userRole);
        if (q == null || q.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query must be at least 2 characters");
        }
        return ResponseEntity.ok(executiveService.searchPatients(q.trim()));
    }

    @GetMapping("/pacientes/{id}")
    public ResponseEntity<PatientSearchResponse> obtener(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @PathVariable Long id) {
        requireEjecutivo(userRole);
        return ResponseEntity.ok(executiveService.getPatient(id));
    }

    @PostMapping("/pacientes")
    public ResponseEntity<PatientSearchResponse> crear(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody CreateGuestPatientRequest request) {
        requireEjecutivo(userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(executiveService.createGuestPatient(request));
    }

    private void requireEjecutivo(String role) {
        if (!"EJECUTIVO".equals(role) && !"ADMINISTRADOR".equals(role) && !"GERENCIA".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }
}
