package com.alfahospital.alfa_auth_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WelcomeGuestMessage {
    private String email;
    private String nombrePaciente;
    private String portalUrl;
    private String phone;
}
