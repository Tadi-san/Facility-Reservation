package com.eagle.app.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            jwtService.parse(token).ifPresent(claims -> setAuthentication(claims, request));
        }
        filterChain.doFilter(request, response);
    }

    private void setAuthentication(Claims claims, HttpServletRequest request) {
        String username = claims.getSubject();
        if (username == null || username.isBlank()) return;

        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        Object roles = claims.get("roles");
        if (roles instanceof List<?> roleList) {
            for (Object role : roleList) {
                if (role instanceof String roleValue && !roleValue.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(roleValue));
                }
            }
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(request.getRemoteAddr());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
