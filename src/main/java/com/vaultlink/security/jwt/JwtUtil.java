package com.vaultlink.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    // -------------------------------------------------------
    // Token Generation
    // -------------------------------------------------------

    /**
     * Generates a JWT token for the given UserDetails (email as subject).
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT token with extra claims embedded.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        log.debug("Generating JWT token for user: {}", userDetails.getUsername());
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())          // subject = email
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // -------------------------------------------------------
    // Token Validation
    // -------------------------------------------------------

    /**
     * Validates the token: checks email match and expiry.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        boolean valid = email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        log.debug("Token validation for {}: {}", email, valid);
        return valid;
    }

    /**
     * Returns true if the token's expiration timestamp is before now.
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // -------------------------------------------------------
    // Claims Extraction
    // -------------------------------------------------------

    /**
     * Extracts the email (subject) from the token.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor using a resolver function.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // -------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------

    /**
     * Parses and returns ALL claims from the given JWT token.
     * Throws JwtException if the token is invalid or tampered.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Builds the signing Key from the Base64-encoded secret in application.properties.
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
