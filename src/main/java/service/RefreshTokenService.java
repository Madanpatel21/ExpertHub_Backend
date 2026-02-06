package service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Repository.RefreshTokenRepo;
import Util.JwtUtil;
import entity.RefreshToken;
import exception.UnauthorizedException;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepo refreshTokenRepo;
    private final JwtUtil jwtUtil;

    public RefreshTokenService(RefreshTokenRepo refreshTokenRepo, JwtUtil jwtUtil) {
        this.refreshTokenRepo = refreshTokenRepo;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Create a new refresh token for a user
     */
    @Transactional
    public RefreshToken createRefreshToken(String email) {
        // Delete existing refresh token for this user
        refreshTokenRepo.deleteByEmail(email);

        String token = JwtUtil.generateRefreshToken(email);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setEmail(email);
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(30)); // 30 days
        refreshToken.setIsRevoked(false);

        return refreshTokenRepo.save(refreshToken);
    }

    /**
     * Verify and get a new access token using refresh token
     */
    @Transactional
    public String refreshAccessToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepo.findByToken(refreshTokenString)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.getIsRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            refreshTokenRepo.delete(refreshToken);
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Generate new access token using user's role
        String role = "USER"; // Default role, you might want to fetch this from database
        return JwtUtil.generateToken(refreshToken.getEmail(), role);
    }

    /**
     * Revoke a refresh token
     */
    @Transactional
    public void revokeRefreshToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepo.findByToken(refreshTokenString)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        refreshToken.setIsRevoked(true);
        refreshTokenRepo.save(refreshToken);
    }

    /**
     * Revoke all refresh tokens for a user
     */
    @Transactional
    public void revokeAllRefreshTokensForUser(String email) {
        refreshTokenRepo.deleteByEmail(email);
    }

    /**
     * Validate refresh token
     */
    public boolean isRefreshTokenValid(String refreshTokenString) {
        Optional<RefreshToken> refreshToken = refreshTokenRepo.findByToken(refreshTokenString);
        return refreshToken.isPresent() &&
                !refreshToken.get().getIsRevoked() &&
                !refreshToken.get().isExpired();
    }
}
