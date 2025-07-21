package com.habitforge.habitforge_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKeyString;

    private Key secretKey;

    @PostConstruct
    public void init() {
        byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);
        this.secretKey = Keys.hmacShaKeyFor(decodedKey);
        System.out.println("[JwtUtil] Secret key initialized");
    }

    public String generateToken(String username) {
        long expirationMillis = 1000 * 60 * 60; // 1 hour
        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
        System.out.println("[JwtUtil] Token generated for user: " + username);
        return token;
    }

    public String extractUsername(String token) {
        String username = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        System.out.println("[JwtUtil] Username extracted from token: " + username);
        return username;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("[JwtUtil] Token validation failed: " + e.getMessage());
            return false;
        }
    }
}




