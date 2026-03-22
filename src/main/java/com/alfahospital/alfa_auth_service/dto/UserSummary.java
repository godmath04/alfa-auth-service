package com.alfahospital.alfa_auth_service.dto;

import com.alfahospital.alfa_auth_service.domain.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UserSummary {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
}