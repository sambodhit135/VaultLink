package com.vaultlink.controller;

import com.vaultlink.dto.request.ChangePasswordRequest;
import com.vaultlink.dto.request.LoginRequest;
import com.vaultlink.dto.request.RegisterRequest;
import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.dto.response.AuthResponse;
import com.vaultlink.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Public endpoint — no token required
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register — email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Public endpoint — no token required
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login — email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/auth/change-password
     * Protected endpoint — valid JWT token required
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("PUT /api/auth/change-password — user: {}", userDetails.getUsername());
        ApiResponse response = authService.changePassword(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
