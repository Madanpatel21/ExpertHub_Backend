package controller;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Util.JwtUtil;
import dto.apiResponse;
import dto.loginRequest;
import dto.otpVerifyRequest;
import dto.registerRequest;
import dto.forgotPasswordRequest;
import dto.resetPasswordRequest;
import entity.RefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import service.authService;
import service.passwordResetService;
import service.RefreshTokenService;
import service.tokenBlacklistService;

@RestController
@RequestMapping("/api/auth")
public class authController {
   private final authService Authservice;
   private final tokenBlacklistService blacklistService;
   private final passwordResetService passwordService;
   private final RefreshTokenService refreshTokenService;

   public authController(authService AuthService, tokenBlacklistService blacklistService,
         passwordResetService passwordService, RefreshTokenService refreshTokenService) {
      this.Authservice = AuthService;
      this.blacklistService = blacklistService;
      this.passwordService = passwordService;
      this.refreshTokenService = refreshTokenService;
   }

   @PostMapping("/register")
   public apiResponse<String> register(@RequestBody registerRequest request) {
      Authservice.register(request);
      return new apiResponse<String>("User registered successfully", true, null);
   }

   @PostMapping("/login")
   public apiResponse<dto.authResponse> login(@RequestBody loginRequest request) {
      // First verify credentials and get OTP
      Authservice.login(request);

      // Return response indicating OTP was sent
      dto.authResponse response = new dto.authResponse();
      response.setMessage("OTP sent to your phone. Please verify to complete login.");

      return new apiResponse<>("Login initiated", true, response);
   }

   @PostMapping("/verify-otp")
   public apiResponse<dto.authResponse> verifyOtp(@RequestBody otpVerifyRequest request) {
      String token = Authservice.verifyOtp(request);
      String email = Authservice.getEmailFromPhone(request.getPhoneNumber());

      dto.authResponse response = new dto.authResponse();
      response.setToken(token);
      response.setEmail(email);
      response.setMessage("Login Successful");

      // Create refresh token
      RefreshToken refreshToken = refreshTokenService.createRefreshToken(email);
      response.setRefreshToken(refreshToken.getToken());

      return new apiResponse<>("Login Successful", true, response);
   }

   @PostMapping("/refresh")
   public apiResponse<dto.authResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
      String newAccessToken = refreshTokenService.refreshAccessToken(request.getRefreshToken());
      String email = JwtUtil.extractEmail(newAccessToken);

      dto.authResponse response = new dto.authResponse();
      response.setToken(newAccessToken);
      response.setEmail(email);
      response.setMessage("Token refreshed successfully");

      // Optionally rotate refresh token (create new one)
      RefreshToken refreshToken = refreshTokenService.createRefreshToken(email);
      response.setRefreshToken(refreshToken.getToken());

      return new apiResponse<>("Token refreshed successfully", true, response);
   }

   @PostMapping("/logout")
   public apiResponse<String> logout(@RequestBody LogoutRequest request) {
      // Revoke refresh token
      if (request.getRefreshToken() != null) {
         refreshTokenService.revokeRefreshToken(request.getRefreshToken());
      }

      String auth = request.getAuthorization();
      if (auth != null && auth.startsWith("Bearer ")) {
         String token = auth.substring(7);
         java.util.Date expiry = new java.util.Date(System.currentTimeMillis() + 0);
         // if parsing expiry fails, still blacklist for a short time
         try {
            expiry = Authservice.getJwtExpiry(token);
         } catch (Exception e) {
            expiry = new java.util.Date(System.currentTimeMillis() + 60_000);
         }
         blacklistService.blacklistToken(token, expiry);
         return new apiResponse<>("Logged out", true, null);
      }
      return new apiResponse<>("Logged out", true, null);
   }

   @PostMapping("/logout-all")
   public apiResponse<String> logoutAll(@RequestBody LogoutAllRequest request) {
      // Revoke refresh token
      if (request.getRefreshToken() != null) {
         refreshTokenService.revokeRefreshToken(request.getRefreshToken());
      }

      String auth = request.getAuthorization();
      if (auth != null && auth.startsWith("Bearer ")) {
         String token = auth.substring(7);
         java.util.Date expiry = new java.util.Date(System.currentTimeMillis() + 0);
         try {
            expiry = Authservice.getJwtExpiry(token);
         } catch (Exception e) {
            expiry = new java.util.Date(System.currentTimeMillis() + 60_000);
         }
         blacklistService.blacklistToken(token, expiry);
      }

      // Revoke all refresh tokens for user
      String email = request.getEmail();
      if (email != null) {
         refreshTokenService.revokeAllRefreshTokensForUser(email);
      }

      return new apiResponse<>("Logged out from all devices", true, null);
   }

   @PostMapping("/forgot-password")
   public apiResponse<String> forgotPassword(@RequestBody forgotPasswordRequest request) {
      String token = passwordService.forgotPassword(request);
      return new apiResponse<String>("Password reset link sent to your email. Token: " + token, true, token);
   }

   @PostMapping("/reset-password")
   public apiResponse<String> resetPassword(@RequestBody resetPasswordRequest request) {
      passwordService.resetPassword(request);
      return new apiResponse<String>("Password reset successfully. Please login with your new password", true, null);
   }

   @PostMapping("/validate-token")
   public apiResponse<Boolean> validateToken(@RequestBody String token) {
      Boolean isValid = passwordService.validateToken(token);
      return new apiResponse<Boolean>("Token is valid", true, isValid);
   }

   // Request/Response classes
   public static class RefreshTokenRequest {
      private String refreshToken;

      public String getRefreshToken() {
         return refreshToken;
      }

      public void setRefreshToken(String refreshToken) {
         this.refreshToken = refreshToken;
      }
   }

   public static class LogoutRequest {
      private String refreshToken;
      private String authorization;

      public String getRefreshToken() {
         return refreshToken;
      }

      public void setRefreshToken(String refreshToken) {
         this.refreshToken = refreshToken;
      }

      public String getAuthorization() {
         return authorization;
      }

      public void setAuthorization(String authorization) {
         this.authorization = authorization;
      }
   }

   public static class LogoutAllRequest {
      private String refreshToken;
      private String authorization;
      private String email;

      public String getRefreshToken() {
         return refreshToken;
      }

      public void setRefreshToken(String refreshToken) {
         this.refreshToken = refreshToken;
      }

      public String getAuthorization() {
         return authorization;
      }

      public void setAuthorization(String authorization) {
         this.authorization = authorization;
      }

      public String getEmail() {
         return email;
      }

      public void setEmail(String email) {
         this.email = email;
      }
   }
}
