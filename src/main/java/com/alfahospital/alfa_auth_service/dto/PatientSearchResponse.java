package com.alfahospital.alfa_auth_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientSearchResponse {
    private Long   id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String idType;
    private String idNumber;
    private String status;
}
