package com.finguard.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${finguard.jwt.secret}")
    private String secret;

    @Value("${finguard.jwt.expiration-ms}")
    private long expirationMs;

    /*
     * Create signing key from configured secret.
     *
     * HS256 requires a sufficiently long secret.
     */
    private SecretKey key() {

        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /*
     * Generate JWT.
     */
    public String generateToken(
            String username,
            String role) {

        Date now = new Date();

        Date expiry =
                new Date(
                        now.getTime() + expirationMs
                );

        return Jwts.builder()

                .subject(username)

                .claim(
                        "role",
                        role
                )

                .issuedAt(now)

                .expiration(expiry)

                .signWith(
                        key(),
                        SignatureAlgorithm.HS256
                )

                .compact();
    }

    /*
     * Extract username from JWT.
     */
    public String extractUsername(
            String token) {

        return Jwts.parser()

                .verifyWith(key())

                .build()

                .parseSignedClaims(token)

                .getPayload()

                .getSubject();
    }

    /*
     * Validate JWT signature and expiration.
     */
    public boolean isTokenValid(
            String token) {

        try {

            Jwts.parser()

                    .verifyWith(key())

                    .build()

                    .parseSignedClaims(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}