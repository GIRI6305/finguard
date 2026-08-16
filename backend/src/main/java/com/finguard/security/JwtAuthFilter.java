package com.finguard.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        /*
         * IMPORTANT:
         *
         * Browser CORS preflight requests do not contain
         * the user's JWT.
         *
         * Never try to authenticate OPTIONS requests.
         */
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {

            filterChain.doFilter(request, response);

            return;
        }

        /*
         * Authentication endpoints are public.
         *
         * Do not try to authenticate:
         *
         * /api/auth/login
         * /api/auth/register
         */
        String path = request.getRequestURI();

        if (path.startsWith("/api/auth/")) {

            filterChain.doFilter(request, response);

            return;
        }

        /*
         * WebSocket handshake is handled separately.
         */
        if (path.startsWith("/ws/")) {

            filterChain.doFilter(request, response);

            return;
        }

        /*
         * Read Authorization header.
         */
        String header =
                request.getHeader("Authorization");

        /*
         * No JWT:
         *
         * Do NOT immediately return 401/403 here.
         *
         * Let Spring Security decide whether the endpoint
         * requires authentication.
         */
        if (header == null ||
                !header.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);

            return;
        }

        /*
         * Extract token.
         */
        String token =
                header.substring(7).trim();

        /*
         * Empty token.
         */
        if (token.isEmpty()) {

            filterChain.doFilter(request, response);

            return;
        }

        /*
         * Validate JWT.
         */
        if (jwtUtil.isTokenValid(token)) {

            try {

                String username =
                        jwtUtil.extractUsername(token);

                /*
                 * Do not overwrite an authentication that
                 * has already been established.
                 */
                if (username != null &&
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication() == null) {

                    /*
                     * Load the real user from the database.
                     */
                    UserDetails userDetails =
                            userDetailsService
                                    .loadUserByUsername(username);

                    /*
                     * Create Spring Security authentication.
                     */
                    UsernamePasswordAuthenticationToken
                            authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    /*
                     * Attach request information.
                     */
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    /*
                     * Store authenticated user.
                     */
                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);
                }

            } catch (Exception ignored) {

                /*
                 * Invalid/malformed JWT must not crash
                 * the filter chain.
                 *
                 * The protected endpoint will subsequently
                 * be rejected by Spring Security.
                 */
                SecurityContextHolder.clearContext();
            }
        }

        /*
         * Continue request processing.
         */
        filterChain.doFilter(request, response);
    }
}