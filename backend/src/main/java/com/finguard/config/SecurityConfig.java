package com.finguard.config;

import java.util.Arrays;
import java.util.List;

import com.finguard.security.CustomUserDetailsService;
import com.finguard.security.JwtAuthFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Value("${finguard.cors.allowed-origins}")
    private String allowedOrigins;

    /*
     * Password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
     * Authentication provider
     *
     * Uses:
     * CustomUserDetailsService -> loads user from database
     * BCryptPasswordEncoder    -> checks password
     */
    @Bean
    public AuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    /*
     * AuthenticationManager used by AuthService during login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationProvider authenticationProvider) {

        return new ProviderManager(authenticationProvider);
    }

    /*
     * Main Spring Security configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            AuthenticationProvider authenticationProvider)
            throws Exception {

        http

            /*
             * REST API does not use server-side sessions.
             */
            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )

            /*
             * JWT API does not need CSRF tokens.
             */
            .csrf(csrf -> csrf.disable())

            /*
             * Enable CORS.
             */
            .cors(cors ->
                    cors.configurationSource(
                            corsConfigurationSource()
                    )
            )

            /*
             * Use our database authentication provider.
             */
            .authenticationProvider(authenticationProvider)

            /*
             * Authorization rules.
             */
            .authorizeHttpRequests(auth -> auth

                /*
                 * Browser CORS preflight MUST be allowed.
                 */
                .requestMatchers(
                        HttpMethod.OPTIONS,
                        "/**"
                ).permitAll()

                /*
                 * Public authentication APIs.
                 */
                .requestMatchers(
                        "/api/auth/**"
                ).permitAll()

                /*
                 * WebSocket handshake.
                 *
                 * The native browser WebSocket client cannot
                 * send a normal Authorization header in the
                 * handshake in the same way Axios does.
                 */
                .requestMatchers(
                        "/ws/**"
                ).permitAll()

                /*
                 * Everything else requires JWT authentication.
                 */
                .anyRequest().authenticated()
            )

            /*
             * JWT filter runs before Spring's username/password
             * authentication filter.
             */
            .addFilterBefore(
                    jwtAuthFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    /*
     * CORS configuration.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        /*
         * Render supplies:
         *
         * CORS_ALLOWED_ORIGINS
         *
         * Example:
         * https://finguard-2cfuexr9e-giri6305s-projects.vercel.app
         */
        configuration.setAllowedOrigins(
                Arrays.stream(
                        allowedOrigins.split(",")
                )
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList()
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "Authorization"
                )
        );

        /*
         * Keep this true because the frontend/backend
         * authentication flow may use credentials.
         */
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}