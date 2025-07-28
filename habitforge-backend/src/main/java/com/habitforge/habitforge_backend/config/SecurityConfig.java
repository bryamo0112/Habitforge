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
                .requestMatchers("/", "/index.html").permitAll()

                .requestMatchers(HttpMethod.POST,
                    "/api/users/login",
                    "/api/users/signup",
                    "/api/users/send-verification-code",
                    "/api/users/verify-code",
                    "/api/users/set-email",
                    "/api/users/forgot-password",
                    "/api/users/reset-password"
                ).permitAll()

                .requestMatchers(HttpMethod.GET, "/api/users/*/profile-picture").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/users/current").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/users/set-username").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/users/*/upload-profile-picture").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/*/mark-prompted").authenticated()
                .requestMatchers("/api/habits/**").authenticated()

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

        // ✅ Allow local frontend dev on any port
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));

        // ✅ Standard methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // ✅ Required headers
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // ✅ Expose Authorization so client can read headers if needed
        config.setExposedHeaders(List.of("Authorization"));

        // ✅ Important for cookies/auth headers
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}





