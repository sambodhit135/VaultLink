package com.vaultlink.service;

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
import com.vaultlink.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RoleEntity testRole;
    private final String testToken = "mock.jwt.token.here";

    @BeforeEach
    void setUp() {
        testRole = new RoleEntity();
        testRole.setId(1L);
        testRole.setRoleName(Role.OWNER);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPassword("hashedPass");
        testUser.setIsActive(true);
        testUser.setRoles(Set.of(testRole));
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        private RegisterRequest request;

        @BeforeEach
        void setUpRequest() {
            request = new RegisterRequest();
            request.setEmail("test@test.com");
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setPassword("rawPassword");
        }

        @Test
        @DisplayName("Should successfully register a new user")
        void test_register_success() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
            when(roleRepository.findByRoleName(any())).thenReturn(Optional.of(testRole));
            when(userRepository.save(any())).thenReturn(testUser);
            
            UserDetails mockUserDetails = mock(UserDetails.class);
            when(userDetailsService.loadUserByUsername(any())).thenReturn(mockUserDetails);
            when(jwtUtil.generateToken(any())).thenReturn(testToken);

            AuthResponse response = authService.register(request);

            assertNotNull(response);
            assertEquals(testToken, response.getToken());
            assertEquals(request.getEmail(), response.getEmail());

            verify(userRepository, times(1)).save(any());
            verify(passwordEncoder, times(1)).encode(any());
        }

        @Test
        @DisplayName("Should throw exception if email already exists")
        void test_register_emailAlreadyExists() {
            when(userRepository.existsByEmail(any())).thenReturn(true);

            assertThrows(RuntimeException.class, () -> authService.register(request));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should encode password and not store raw password")
        void test_register_passwordIsHashed() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
            when(roleRepository.findByRoleName(any())).thenReturn(Optional.of(testRole));
            when(userRepository.save(any())).thenReturn(testUser);
            
            UserDetails mockUserDetails = mock(UserDetails.class);
            when(userDetailsService.loadUserByUsername(any())).thenReturn(mockUserDetails);
            when(jwtUtil.generateToken(any())).thenReturn(testToken);

            authService.register(request);

            verify(passwordEncoder, times(1)).encode(request.getPassword());
        }

        @Test
        @DisplayName("Should assign default Role.OWNER to new users")
        void test_register_defaultRoleIsOwner() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
            when(roleRepository.findByRoleName(Role.OWNER)).thenReturn(Optional.of(testRole));
            when(userRepository.save(any())).thenReturn(testUser);
            
            UserDetails mockUserDetails = mock(UserDetails.class);
            when(userDetailsService.loadUserByUsername(any())).thenReturn(mockUserDetails);
            when(jwtUtil.generateToken(any())).thenReturn(testToken);

            authService.register(request);

            verify(roleRepository, times(1)).findByRoleName(Role.OWNER);
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        private LoginRequest request;

        @BeforeEach
        void setUpRequest() {
            request = new LoginRequest();
            request.setEmail("test@test.com");
            request.setPassword("password123");
        }

        @Test
        @DisplayName("Should successfully log in user")
        void test_login_success() {
            Authentication mockAuth = mock(Authentication.class);
            when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));
            
            UserDetails mockUserDetails = mock(UserDetails.class);
            when(userDetailsService.loadUserByUsername(any())).thenReturn(mockUserDetails);
            when(jwtUtil.generateToken(any())).thenReturn(testToken);

            AuthResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals(testToken, response.getToken());
            assertEquals(testUser.getEmail(), response.getEmail());
        }

        @Test
        @DisplayName("Should throw BadCredentialsException on invalid credentials")
        void test_login_invalidCredentials() {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad creds"));

            assertThrows(BadCredentialsException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("Should throw exception if user is not found after auth succeeds")
        void test_login_userNotFoundAfterAuth() {
            Authentication mockAuth = mock(Authentication.class);
            when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> authService.login(request));
        }
    }

    @Nested
    @DisplayName("ChangePassword Tests")
    class ChangePasswordTests {

        private ChangePasswordRequest request;

        @BeforeEach
        void setUpRequest() {
            request = new ChangePasswordRequest();
            request.setCurrentPassword("oldPassword");
            request.setNewPassword("newPassword123");
            request.setConfirmPassword("newPassword123");
        }

        @Test
        @DisplayName("Should successfully change password")
        void test_changePassword_success() {
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("oldPassword", testUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("newPassword123")).thenReturn("newHashedPassword");

            ApiResponse response = authService.changePassword(request, testUser.getEmail());

            assertNotNull(response);
            assertTrue(response.getSuccess());
            verify(userRepository, times(1)).save(testUser);
        }

        @Test
        @DisplayName("Should throw exception if current password is wrong")
        void test_changePassword_wrongCurrentPassword() {
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("oldPassword", testUser.getPassword())).thenReturn(false);

            assertThrows(RuntimeException.class, () -> authService.changePassword(request, testUser.getEmail()));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception if new passwords do not match")
        void test_changePassword_passwordsDoNotMatch() {
            request.setConfirmPassword("differentPassword");
            
            // Note: Validation might happen before DB lookup depending on service logic
            // Stubbing userRepository just in case it reaches there
            lenient().when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

            assertThrows(RuntimeException.class, () -> authService.changePassword(request, testUser.getEmail()));
            verify(userRepository, never()).save(any());
        }
    }
}
