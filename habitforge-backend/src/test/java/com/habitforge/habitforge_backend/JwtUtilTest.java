package com.habitforge.habitforge_backend;
import com.habitforge.habitforge_backend.security.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.security.Key;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    JwtUtil jwtUtil;

    private final String secretKeyBase64 = Base64.getEncoder()
            .encodeToString("supersecretkeysupersecretkeysupersecretk".getBytes());

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        setPrivateField(jwtUtil, "secretKeyString", secretKeyBase64);
        jwtUtil.init();
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void generateToken_WithEmail_ShouldContainUsernameAndEmail() {
        String username = "testuser";
        String email = "testuser@example.com";

        String token = jwtUtil.generateToken(username, email);

        assertNotNull(token);
        String extractedUsername = jwtUtil.extractUsername(token);
        String extractedEmail = jwtUtil.extractEmail(token);

        assertEquals(username, extractedUsername);
        assertEquals(email, extractedEmail);
    }

    @Test
    void generateToken_WithoutEmail_ShouldContainUsernameAndNullEmail() {
        String username = "testuser";

        String token = jwtUtil.generateToken(username);

        assertNotNull(token);
        String extractedUsername = jwtUtil.extractUsername(token);
        String extractedEmail = jwtUtil.extractEmail(token);

        assertEquals(username, extractedUsername);
        assertNull(extractedEmail);
    }

    @Test
    void extractUsernameFromHeader_ShouldReturnUsername_WhenValidHeader() {
        String username = "testuser";
        String token = jwtUtil.generateToken(username);
        String header = "Bearer " + token;

        String extracted = jwtUtil.extractUsernameFromHeader(header);
        assertEquals(username, extracted);
    }

    @Test
    void extractUsernameFromHeader_ShouldReturnNull_WhenInvalidHeader() {
        assertNull(jwtUtil.extractUsernameFromHeader(null));
        assertNull(jwtUtil.extractUsernameFromHeader("InvalidHeader"));
        assertNull(jwtUtil.extractUsernameFromHeader("BearerX token"));
    }

    @Test
    void validateToken_ShouldReturnTrue_ForValidToken() {
        String token = jwtUtil.generateToken("user");
        assertTrue(jwtUtil.validateToken(token));
    }

   @Test
void validateToken_ShouldReturnFalse_ForExpiredToken() throws Exception {
    JwtUtil shortLivedJwtUtil = new JwtUtil();
    setPrivateField(shortLivedJwtUtil, "secretKeyString", secretKeyBase64);
    shortLivedJwtUtil.init();

    String token = Jwts.builder()
            .setSubject("user")
            .setExpiration(new java.util.Date(System.currentTimeMillis() - 1000)) // expired 1 sec ago
            .signWith((Key) getPrivateField(shortLivedJwtUtil, "secretKey"), SignatureAlgorithm.HS256)
            .compact();

    boolean valid = shortLivedJwtUtil.validateToken(token);
    assertFalse(valid);
}


    private Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    void validateToken_ShouldReturnFalse_ForInvalidToken() {
        String invalidToken = "this.is.not.a.token";
        assertFalse(jwtUtil.validateToken(invalidToken));
    }

    @Test
    void extractUsername_ShouldReturnNull_ForInvalidToken() {
        assertNull(jwtUtil.extractUsername("invalid.token"));
    }

    @Test
    void extractEmail_ShouldReturnNull_ForInvalidToken() {
        assertNull(jwtUtil.extractEmail("invalid.token"));
    }
}

