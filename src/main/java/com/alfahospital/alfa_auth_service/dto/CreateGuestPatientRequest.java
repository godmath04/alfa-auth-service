package com.alfahospital.alfa_auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
    private String idType;

    @NotBlank
    private String idNumber;

    private String phone;
    private String birthDate;
    private String city;
    private String gender;
}
