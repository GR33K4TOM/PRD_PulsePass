package com.example.PRD_PulsePass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class JwtService {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        String base64 = jwtProperties.getSecretBase64();
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException("PULSEPASS_JWT_SECRET_BASE64 must be configured and not blank");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(base64);
        } catch (Exception e) {
            throw new IllegalStateException("PULSEPASS_JWT_SECRET_BASE64 is not valid Base64", e);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret key must be at least 32 bytes (256 bits) for HS256");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getAccessTokenTtl());

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .id(UUID.randomUUID().toString())
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims validateAndParseClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token);

        if (!Jwts.SIG.HS256.getId().equals(jws.getHeader().getAlgorithm())) {
            throw new SecurityException("Unexpected JWT algorithm: " + jws.getHeader().getAlgorithm());
        }

        Claims claims = jws.getPayload();
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new SecurityException("JWT subject (sub) is required");
        }
        if (claims.getExpiration() == null || claims.getExpiration().before(new Date())) {
            throw new SecurityException("JWT expiration (exp) is required and must be in the future");
        }
        if (claims.getId() == null || claims.getId().isBlank()) {
            throw new SecurityException("JWT ID (jti) is required");
        }
        if (claims.getIssuedAt() == null) {
            throw new SecurityException("JWT issuedAt (iat) is required");
        }

        return claims;
    }

    public Long extractUserId(String token) {
        Claims claims = validateAndParseClaims(token);
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new SecurityException("Invalid user ID in JWT subject", e);
        }
    }

    public long getExpiresInSeconds() {
        return jwtProperties.getAccessTokenTtl().toSeconds();
    }
}
