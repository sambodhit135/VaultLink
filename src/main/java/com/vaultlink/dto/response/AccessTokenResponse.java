package com.vaultlink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessTokenResponse {

    private Long id;
    private String token;
    private String sharedWithEmail;
    private LocalDateTime expiresAt;
    private Boolean isUsed;
    private String documentTitle;
}
