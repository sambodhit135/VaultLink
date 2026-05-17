package com.vaultlink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String message;
}
