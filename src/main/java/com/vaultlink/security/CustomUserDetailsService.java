package com.vaultlink.security;

import com.vaultlink.entity.User;
import com.vaultlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by email for Spring Security authentication.
     * The "username" in Spring Security context is the user's email in VaultLink.
     *
     * @param email the email address used as the login identifier
     * @return UserDetails built from the database User entity
     * @throws UsernameNotFoundException if no user exists with the given email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        // Build granted authorities from the user's roles, prefixed with "ROLE_"
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleName().name()))
                .collect(Collectors.toList());

        log.debug("Successfully loaded user '{}' with authorities: {}", email, authorities);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
