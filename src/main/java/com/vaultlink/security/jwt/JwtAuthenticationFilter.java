package com.vaultlink.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Step 1: Read the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // Step 2: Skip if header is missing or does not start with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found in request to: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Extract the raw JWT token (strip "Bearer " prefix)
        final String token = authHeader.substring(7);

        // Step 4: Extract email from token
        String email = null;
        try {
            email = jwtUtil.extractEmail(token);
        } catch (Exception e) {
            log.warn("Failed to extract email from JWT token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Step 5: If email is present AND no auth is set in SecurityContext yet
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Step 5a: Load UserDetails from the database
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Step 5b: Validate the token
            if (jwtUtil.isTokenValid(token, userDetails)) {

                // Step 5c: Create authentication token
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                          // credentials null after auth
                                userDetails.getAuthorities()
                        );

                // Step 5d: Attach request details (IP, session, etc.)
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Step 5e: Register authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWT valid — authenticated user '{}' for request: {}",
                        email, request.getRequestURI());

            } else {
                log.warn("JWT token invalid or expired for user '{}' on request: {}",
                        email, request.getRequestURI());
            }
        }

        // Step 6: Always continue the filter chain
        filterChain.doFilter(request, response);
    }
}
