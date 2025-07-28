package com.habitforge.habitforge_backend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private static final Logger LOGGER = Logger.getLogger(JwtFilter.class.getName());

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String method = request.getMethod();
        LOGGER.info("🔐 JwtFilter: " + method + " " + path);

        if (shouldNotFilter(request)) {
            LOGGER.info("✅ Public endpoint, skipping filter");
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            try {
                String username = jwtUtil.extractUsername(jwt);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    if (jwtUtil.validateToken(jwt)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        username,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                                );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        LOGGER.info("✅ Authenticated user: " + username);
                    } else {
                        LOGGER.warning("❌ Invalid token");
                    }
                }
            } catch (ExpiredJwtException e) {
                LOGGER.warning("⏰ Token expired");
            } catch (JwtException e) {
                LOGGER.warning("❌ Invalid token format: " + e.getMessage());
            }
        } else {
            LOGGER.warning("⚠️ No token provided or malformed header");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath().replaceAll("/+$", "");

        return path.equals("/") ||
               path.equals("/favicon.ico") ||
               path.equals("/index.html") ||
               path.startsWith("/static/") ||
               path.startsWith("/public/") ||

               // Public auth & verification endpoints
               path.equals("/api/users/login") ||
               path.equals("/api/users/signup") ||
               path.equals("/api/users/send-verification-code") ||
               path.equals("/api/users/verify-login-code") ||
               path.equals("/api/users/verify-code") ||
               path.equals("/api/users/set-email") ||  
               path.equals("/api/users/forgot-password") ||
               path.equals("/api/users/reset-password") ||

               // Public profile picture endpoint
               path.matches("^/api/users/[^/]+/profile-picture$") ||

               // Allow CORS preflight requests
               request.getMethod().equalsIgnoreCase("OPTIONS");
    }
}

















