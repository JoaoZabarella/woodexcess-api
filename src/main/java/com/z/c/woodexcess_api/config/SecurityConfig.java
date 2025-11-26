package com.z.c.woodexcess_api.config;

import com.z.c.woodexcess_api.security.JwtAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired(required = false)
    private UsernamePasswordAuthenticationFilter jwtAuthenticationFilter; // optional if present in project

    @Autowired
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
                // Public endpoints
                .requestMatchers(HttpMethod.GET, "/api/listings", "/api/listings/*").permitAll()
                .requestMatchers("/api/users/register", "/api/auth/**").permitAll()
                // Protected endpoints
                .requestMatchers(HttpMethod.POST, "/api/listings").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/listings/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/listings/**").authenticated()
                .anyRequest().authenticated()
            .and()
            .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint);

        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }
}