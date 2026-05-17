package com.vaultlink.service.impl;

import com.vaultlink.dto.request.ChangePasswordRequest;
import com.vaultlink.dto.request.LoginRequest;
import com.vaultlink.dto.request.RegisterRequest;
import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.dto.response.AuthResponse;
import com.vaultlink.entity.RoleEntity;
import com.vaultlink.entity.User;
import com.vaultlink.enums.Role;
import com.vaultlink.repository.RoleRepository;
import com.vaultlink.repository.UserRepository;
import com.vaultlink.security.jwt.JwtUtil;
import com.vaultlink.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    // -------------------------------------------------------
    // REGISTER
    // -------------------------------------------------------

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Step 1 & 2: Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email already registered: {}", request.getEmail());
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        // Step 3 & 4 & 5: Build the User entity
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .build();

        // Step 6: Find or create the OWNER role
        RoleEntity ownerRole = roleRepository.findByRoleName(Role.OWNER)
                .orElseGet(() -> {
                    log.info("OWNER role not found — creating it");
                    return roleRepository.save(
                            RoleEntity.builder().roleName(Role.OWNER).build()
                    );
                });

        user.setRoles(Set.of(ownerRole));

        // Step 7: Persist user
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Step 8: Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        log.debug("JWT token generated for new user: {}", savedUser.getEmail());

        // Step 9: Build and return AuthResponse
        return AuthResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(Role.OWNER.name())
                .message("Registration successful")
                .build();
    }

    // -------------------------------------------------------
    // LOGIN
    // -------------------------------------------------------

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmail());

        // Step 1: Authenticate — throws BadCredentialsException automatically if wrong
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (org.springframework.security.core.AuthenticationException ex) {
            log.error("Login failed for: {}", request.getEmail(), ex);
            throw ex;
        }

        // Step 2: Load user from DB
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getEmail()));

        // Step 3: Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        // Resolve role name for response (use first role, default to OWNER)
        String roleName = user.getRoles().stream()
                .findFirst()
                .map(r -> r.getRoleName().name())
                .orElse(Role.OWNER.name());

        log.info("Login successful for: {}", user.getEmail());

        // Step 4: Return AuthResponse
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(roleName)
                .message("Login successful")
                .build();
    }

    // -------------------------------------------------------
    // CHANGE PASSWORD
    // -------------------------------------------------------

    @Override
    @Transactional
    public ApiResponse changePassword(ChangePasswordRequest request, String email) {
        log.info("Password change requested for user: {}", email);

        // Step 1: Load user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        // Step 2: Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change failed — current password incorrect for: {}", email);
            throw new RuntimeException("Current password is incorrect");
        }

        // Step 4: Check new password matches confirm password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Password change failed — new passwords do not match for: {}", email);
            throw new RuntimeException("Passwords do not match");
        }

        // Step 6: Encode and save the new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", email);

        // Step 7: Return success response
        return ApiResponse.success("Password changed successfully");
    }
}
