package com.example.PRD_PulsePass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String VALID_SECRET_BASE64 = "dGVzdC1vbmx5LW5vdC1mb3ItcHJvZHVjdGlvbi1zZWNyZXQta2V5LTIwMjYtMzJieXRlcw==";
    private JwtProperties jwtProperties;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setIssuer("pulsepass-api");
        jwtProperties.setSecretBase64(VALID_SECRET_BASE64);
        jwtProperties.setAccessTokenTtl(Duration.ofSeconds(900));
        jwtService = new JwtService(jwtProperties);
        jwtService.init();
    }

    @Test
    void generateAndValidate_validToken_returnsClaimsAndUserId() {
        String token = jwtService.generateToken(42L);

        assertThat(token).isNotBlank();
        Claims claims = jwtService.validateAndParseClaims(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuer()).isEqualTo("pulsepass-api");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(new Date());
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.getExpiresInSeconds()).isEqualTo(900L);
    }

    @Test
    void validate_expiredToken_throwsExpiredJwtException() {
        jwtProperties.setAccessTokenTtl(Duration.ofSeconds(-10));
        JwtService expiredJwtService = new JwtService(jwtProperties);
        expiredJwtService.init();

        String expiredToken = expiredJwtService.generateToken(10L);

        assertThatThrownBy(() -> jwtService.validateAndParseClaims(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void validate_wrongIssuer_throwsIncorrectClaimException() {
        JwtProperties otherProps = new JwtProperties();
        otherProps.setIssuer("wrong-issuer");
        otherProps.setSecretBase64(VALID_SECRET_BASE64);
        otherProps.setAccessTokenTtl(Duration.ofSeconds(900));
        JwtService otherService = new JwtService(otherProps);
        otherService.init();

        String token = otherService.generateToken(10L);

        assertThatThrownBy(() -> jwtService.validateAndParseClaims(token))
                .isInstanceOf(IncorrectClaimException.class);
    }

    @Test
    void validate_tamperedToken_throwsSignatureException() {
        String token = jwtService.generateToken(10L);
        String[] parts = token.split("\\.");
        String tamperedSig = parts[2].substring(0, parts[2].length() - 2) + (parts[2].endsWith("AA") ? "BB" : "AA");
        String tamperedToken = parts[0] + "." + parts[1] + "." + tamperedSig;

        assertThatThrownBy(() -> jwtService.validateAndParseClaims(tamperedToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void validate_emptyOrBlankToken_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> jwtService.validateAndParseClaims(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwtService.validateAndParseClaims("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void init_weakKey_throwsIllegalStateException() {
        JwtProperties weakProps = new JwtProperties();
        weakProps.setIssuer("pulsepass-api");
        weakProps.setSecretBase64("c2hvcnRrZXk="); // "shortkey" < 32 bytes
        JwtService weakService = new JwtService(weakProps);

        assertThatThrownBy(weakService::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void init_blankKey_throwsIllegalStateException() {
        JwtProperties emptyProps = new JwtProperties();
        emptyProps.setSecretBase64(" ");
        JwtService emptyService = new JwtService(emptyProps);

        assertThatThrownBy(emptyService::init)
                .isInstanceOf(IllegalStateException.class);
    }
}
