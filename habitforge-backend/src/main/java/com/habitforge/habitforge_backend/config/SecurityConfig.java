package com.habitforge.habitforge_backend.config;

import com.habitforge.habitforge_backend.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .formLogin(form -> form.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public routes
                .requestMatchers("/", "/index.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users/signup", "/api/users/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/*/profile-picture").permitAll()

                // Allow preflight CORS requests
                .requestMatchers(HttpMethod.OPTIONS, "/api/users/**", "/api/habits/**").permitAll()

                // Explicit matcher for edit
                .requestMatchers(HttpMethod.PUT, "/api/habits/*/edit").authenticated()
                .requestMatchers(HttpMethod.OPTIONS, "/api/habits/*/edit").permitAll()

                // Authenticated user-specific routes
                .requestMatchers(HttpMethod.POST, "/api/users/*/upload-profile-picture").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/*/mark-prompted").authenticated()

                // General habit routes (fallback)
                .requestMatchers(HttpMethod.GET, "/api/habits/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/habits/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/habits/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/habits/**").authenticated()

                // Everything else denied
                .anyRequest().denyAll()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
