package com.alfahospital.alfa_auth_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String idType;
    private String idNumber;
    private String birthDate;
    private String city;
    private String gender;
    private String role;
    private boolean hasProfilePhoto;
}

