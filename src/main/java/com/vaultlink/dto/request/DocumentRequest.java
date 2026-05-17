package com.vaultlink.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private LocalDate issueDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private String filePath;
}
