package com.alfahospital.alfa_auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredMessage {
    private Long   pacienteId;
    private String pacienteEmail;
    private String idNumber;
}
