package service;

import java.io.IOException;

import java.util.Collections;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import Util.JwtUtil;

import Repository.userRepository;
import entity.user;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final tokenBlacklistService blacklistService;
    private final userRepository userRepo;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, tokenBlacklistService blacklistService, userRepository userRepo) {
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
        this.userRepo = userRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isTokenValid(token) && !blacklistService.isBlacklisted(token)) {
                String email = jwtUtil.extractEmail(token);
                user User = userRepo.findByEmail(email).orElse(null);
                if (User != null) {
                    String role = User.getRole() != null ? User.getRole().getName() : "USER";
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
