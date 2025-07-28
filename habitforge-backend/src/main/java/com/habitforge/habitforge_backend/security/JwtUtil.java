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
        try {
            byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);
            this.secretKey = Keys.hmacShaKeyFor(decodedKey);
            System.out.println("[JwtUtil] Secret key initialized");
        } catch (Exception e) {
            System.out.println("[JwtUtil] Failed to initialize secret key: " + e.getMessage());
        }
    }

    // Generate token with optional email
    public String generateToken(String username, String email) {
        long expirationMillis = 1000 * 60 * 60; // 1 hour
        try {
            return Jwts.builder()
                    .setSubject(username)
                    .claim("email", email)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                    .signWith(secretKey, SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            System.out.println("[JwtUtil] Failed to generate token: " + e.getMessage());
            return null;
        }
    }

    // Overload without email
    public String generateToken(String username) {
        return generateToken(username, null);
    }

    // Extract username from JWT
    public String extractUsername(String token) {
        try {
            return getAllClaims(token).getSubject();
        } catch (JwtException e) {
            System.out.println("[JwtUtil] Failed to extract username: " + e.getMessage());
            return null;
        }
    }

    // Extract email if present
    public String extractEmail(String token) {
        try {
            return getAllClaims(token).get("email", String.class);
        } catch (Exception e) {
            System.out.println("[JwtUtil] Failed to extract email: " + e.getMessage());
            return null;
        }
    }

    // Extract username from Authorization header
    public String extractUsernameFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("[JwtUtil] Missing or invalid Authorization header.");
            return null;
        }

        String token = authHeader.substring(7);
        return extractUsername(token);
    }

    // Validate token integrity and expiration
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("[JwtUtil] Token expired: " + e.getMessage());
        } catch (JwtException e) {
            System.out.println("[JwtUtil] Invalid JWT: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("[JwtUtil] Token is null or empty: " + e.getMessage());
        }
        return false;
    }

    // Private: Extract all claims (subject, email, expiration, etc.)
    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Debug utility
    public void printDecodedToken(String token) {
        try {
            Claims claims = getAllClaims(token);
            System.out.println("[JwtUtil] Decoded token claims:");
            System.out.println("  Subject (username): " + claims.getSubject());
            System.out.println("  Email: " + claims.get("email"));
            System.out.println("  Expiration: " + claims.getExpiration());
        } catch (Exception e) {
            System.out.println("[JwtUtil] Token decode failed: " + e.getMessage());
        }
    }
}








