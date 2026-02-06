package service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Repository.passwordResetTokenRepository;
import Repository.userRepository;
import dto.forgotPasswordRequest;
import dto.resetPasswordRequest;
import entity.passwordResetToken;
import entity.user;
import exception.badRequestException;

@Service
public class passwordResetService {
    
    @Autowired
    private userRepository userRepo;
    
    @Autowired
    private passwordResetTokenRepository tokenRepo;
    
    private static final int TOKEN_EXPIRY_MINUTES = 30;
    
    /**
     * Initiate forgot password by generating a reset token
     */
    public String forgotPassword(forgotPasswordRequest request) {
        user User = userRepo.findByEmail(request.getEmail())
            .orElseThrow(() -> new badRequestException("User with this email doesn't exist"));
        
        // Delete any existing unused tokens for this user
        tokenRepo.findByUserIdAndIsUsedFalse(User.getId())
            .ifPresent(tokenRepo::delete);
        
        // Generate a unique token
        String token = UUID.randomUUID().toString();
        
        // Create password reset token
        passwordResetToken resetToken = new passwordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(User);
        resetToken.setExpiryTime(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
        resetToken.setIsUsed(false);
        
        tokenRepo.save(resetToken);
        
        // In a real application, send this token via email
        // For now, we'll return it (in production, send via email)
        System.out.println("Password Reset Token: " + token);
        System.out.println("Token expires in " + TOKEN_EXPIRY_MINUTES + " minutes");
        
        return token;
    }
    
    /**
     * Verify and reset password using token
     */
    public void resetPassword(resetPasswordRequest request) {
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new badRequestException("Passwords do not match");
        }
        
        // Validate password strength
        if (request.getNewPassword().length() < 6) {
            throw new badRequestException("Password must be at least 6 characters long");
        }
        
        // Find the token
        passwordResetToken resetToken = tokenRepo.findByToken(request.getToken())
            .orElseThrow(() -> new badRequestException("Invalid reset token"));
        
        // Check if token is already used
        if (resetToken.getIsUsed()) {
            throw new badRequestException("This reset token has already been used");
        }
        
        // Check if token has expired
        if (resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new badRequestException("Reset token has expired. Please request a new one");
        }
        
        // Update user password
        user User = resetToken.getUser();
        User.setPassword(request.getNewPassword());
        userRepo.save(User);
        
        // Mark token as used
        resetToken.setIsUsed(true);
        tokenRepo.save(resetToken);
    }
    
    /**
     * Validate if a token is still valid
     */
    public Boolean validateToken(String token) {
        passwordResetToken resetToken = tokenRepo.findByToken(token)
            .orElseThrow(() -> new badRequestException("Invalid token"));
        
        if (resetToken.getIsUsed()) {
            throw new badRequestException("Token has already been used");
        }
        
        if (resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new badRequestException("Token has expired");
        }
        
        return true;
    }
    
    /**
     * Clean up expired tokens (can be called periodically)
     */
    public void cleanupExpiredTokens() {
        tokenRepo.deleteByExpiryTimeBefore(LocalDateTime.now());
    }
}
