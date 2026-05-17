package com.vaultlink.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareDocumentRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Must be a valid email address")
    private String sharedWithEmail;

    @Builder.Default
    private Integer expiresInHours = 24;
}
