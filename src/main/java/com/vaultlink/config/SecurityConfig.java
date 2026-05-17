package com.vaultlink.config;

import com.vaultlink.security.CustomUserDetailsService;
import com.vaultlink.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    // -------------------------------------------------------
    // Public endpoints — no authentication required
    // -------------------------------------------------------
    private static final String[] PUBLIC_URLS = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/documents/shared/**",
            "/actuator/health",
            // Swagger / OpenAPI endpoints
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error",
            // Static Resources
            "/",
            "/login.html",
            "/register.html",
            "/dashboard.html",
            "/dashboard",
            "/documents.html",
            "/documents",
            "/add-document.html",
            "/add-document",
            "/notifications.html",
            "/notifications",
            "/profile.html",
            "/profile",
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico"
    };

    // -------------------------------------------------------
    // Security Filter Chain
    // -------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — JWT is stateless, no session cookies
                .csrf(AbstractHttpConfigurer::disable)

                // Configure CORS
                .cors(cors -> cors.configurationSource(request -> {
                    org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOrigins(java.util.List.of("*"));
                    config.setAllowedMethods(java.util.List.of("GET","POST","PUT","DELETE","OPTIONS"));
                    config.setAllowedHeaders(java.util.List.of("*"));
                    return config;
                }))

                // Stateless session — no HttpSession created or used
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // URL authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated()
                )

                // Register the custom AuthenticationProvider
                .authenticationProvider(authenticationProvider())

                // Add JWT filter before Spring's default username/password filter
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // -------------------------------------------------------
    // Password Encoder — BCrypt with default strength (10)
    // -------------------------------------------------------

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // -------------------------------------------------------
    // Authentication Provider — DAO with UserDetailsService + BCrypt
    // -------------------------------------------------------

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Spring Security 6.x: UserDetailsService is passed via constructor
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // -------------------------------------------------------
    // Authentication Manager — from Spring's AuthenticationConfiguration
    // -------------------------------------------------------

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
