package com.alfahospital.alfa_auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Pattern(regexp = "^\\+?\\d{7,15}$", message = "Número de teléfono inválido")
    private String phone;

    @NotBlank
    @Pattern(regexp = "^(cedula|passport)$", message = "Tipo de documento inválido")
    private String idType;

    @NotBlank
    private String idNumber;

    @NotBlank
    private String birthDate;

    @NotBlank
    private String city;

    @NotBlank
    @Pattern(regexp = "^(M|F|O)$", message = "Género inválido")
    private String gender;
}
