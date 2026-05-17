package com.vaultlink.service;

import com.vaultlink.dto.request.ChangePasswordRequest;
import com.vaultlink.dto.request.LoginRequest;
import com.vaultlink.dto.request.RegisterRequest;
import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    ApiResponse changePassword(ChangePasswordRequest request, String email);
}
