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
 * Компонент для создания и валидации JSON Web Token (JWT).
 * Используется библиотека io.jsonwebtoken (JJWT).
 * Генерирует access и refresh токены, валидирует токены, извлекает данные из токенов.
 */
@Component
public class JwtTokenProvider {
    //Симметричный секрет, которым подписываются токены (HS256).
    @Value("${jwt.secret}")
    private String jwtSecret;

    //Срок действия access токена в миллисекундах.
    @Getter
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    //Срок действия refresh токена в миллисекундах.
    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration;

    //Генерация симметричного ключа для подписи токенов.
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    //Генерация access токена.
    public String generateAccessToken(String username, Role role) {
        return generateToken(username, role, jwtExpiration);
    }

    //Генерация refresh токена.
    public String generateRefreshToken(String username, Role role) {
        return generateToken(username, role, refreshExpiration);
    }

    // Генерация JWT токена с указанным сроком действия.
    // В токен добавляются: имя пользователя (subject), роль, время создания и время истечения.
    // Токен подписывается симметричным ключом с использованием алгоритма HS256.
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

    // Валидация JWT токена: проверка подписи и срока действия.
    // Возвращает true, если токен валиден, false - если токен поврежден, истек или имеет неверную подпись.
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

    // Извлекает имя пользователя (email) из JWT токена (поле subject).
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    // Извлекает роль пользователя из JWT токена (поле "role" в claims).
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }

    // Извлекает дату истечения срока действия токена.
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getExpiration();
    }
}
