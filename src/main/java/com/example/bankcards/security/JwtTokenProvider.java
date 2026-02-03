package com.example.bankcards.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.example.bankcards.entity.Role;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Component for creating and validating JSON Web Token (JWT).
 * Uses io.jsonwebtoken (JJWT) library.
 * Generates access and refresh tokens, validates tokens, extracts data from tokens.
 */
@Component
public class JwtTokenProvider {
    // Symmetric secret used to sign tokens (HS256).
    @Value("${jwt.secret}")
    private String jwtSecret;

    // Access token expiration time in milliseconds.
    @Getter
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // Refresh token expiration time in milliseconds.
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // Generation of symmetric key for token signing.
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Generation of access token.
    public String generateAccessToken(String username, Role role) {
        return generateToken(username, role, jwtExpiration);
    }

    // Generation of refresh token.
    public String generateRefreshToken(String username, Role role) {
        return generateToken(username, role, refreshExpiration);
    }

    // Generation of JWT token with specified expiration time.
    // Token includes: username (subject), role, creation time and expiration time.
    // Token is signed with symmetric key using HS256 algorithm.
    private String generateToken(String username, Role role, long jwtExpiration) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(username)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // JWT token validation: checks signature and expiration time.
    // Returns true if token is valid, false if token is corrupted, expired or has invalid signature.
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Extracts username (email) from JWT token (subject field).
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    // Extracts user role from JWT token ("role" field in claims).
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }

    // Extracts token expiration date.
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getExpiration();
    }
}
