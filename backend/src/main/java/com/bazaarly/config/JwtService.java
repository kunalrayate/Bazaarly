package com.bazaarly.config;

import com.bazaarly.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiryMs;
    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiry-hours}") long hours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMs = hours * 3600_000L;
    }
    public String generate(User u) {
        return Jwts.builder().subject(String.valueOf(u.getId())).claim("role", u.getRole().name())
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiryMs)).signWith(key).compact();
    }
    /** @return user id or null if the token is invalid/expired */
    public Long parse(String token) {
        try { return Long.valueOf(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject()); }
        catch (Exception e) { return null; }
    }
}
