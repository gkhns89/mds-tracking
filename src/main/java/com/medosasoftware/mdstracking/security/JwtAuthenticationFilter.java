package com.medosasoftware.mdstracking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        System.out.println("🔍 Processing request: " + requestURI);

        String token = resolveToken(request);
        System.out.println("🔑 Token extracted: " + (token != null ? "YES (length: " + token.length() + ")" : "NO"));

        if (token != null) {
            boolean isValid = jwtTokenProvider.validateToken(token);
            System.out.println("✅ Token valid: " + isValid);

            if (isValid) {
                String email = jwtTokenProvider.getEmailFromToken(token);
                System.out.println("📧 Email from token: " + email);

                if (email != null) {
                    try {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                        System.out.println("👤 User found: " + userDetails.getUsername() + " with authorities: " + userDetails.getAuthorities());

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        System.out.println("🔐 Authentication set successfully!");
                    } catch (Exception e) {
                        System.out.println("❌ Error loading user: " + e.getMessage());
                    }
                } else {
                    System.out.println("❌ Email is null from token");
                }
            } else {
                System.out.println("❌ Token validation failed");
            }
        } else {
            System.out.println("❌ No token found in request");
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        System.out.println("🎯 Authorization header: " + bearerToken);

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}