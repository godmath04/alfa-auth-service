package com.alfahospital.alfa_auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGuestPatientRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^(cedula|passport)$", message = "Tipo de documento inválido")
    private String idType;

    @NotBlank
    private String idNumber;

    @NotBlank
    @Pattern(regexp = "^\\+?\\d{7,15}$", message = "Número de teléfono inválido")
    private String phone;

    @NotBlank
    private String birthDate;

    @NotBlank
    private String city;

    @NotBlank
    @Pattern(regexp = "^(M|F|O)$", message = "Género inválido")
    private String gender;
}
