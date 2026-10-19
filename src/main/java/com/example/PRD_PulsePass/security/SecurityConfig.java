package com.example.PRD_PulsePass.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            AuthenticationEntryPoint authEntryPoint,
            AccessDeniedHandler deniedHandler
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(deniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/venues/active", "/api/venues/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/artists/active", "/api/artists/by-stage-name", "/api/artists/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/published", "/api/events/by-artist").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/*/tickets/paid").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/events/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/events/*/publish").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/artists/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tickets").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/tickets/by-user").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tickets/*").hasAnyRole("USER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/cancel").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/use").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/by-email", "/api/users/by-username").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
