package com.alfahospital.alfa_auth_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    private String firstName;
    private String lastName;
    private String phone;
    private String city;
    private String gender;
    private String birthDate;
}

