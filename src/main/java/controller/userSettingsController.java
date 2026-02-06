package controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dto.apiResponse;
import dto.userProfileSettingsRequest;
import dto.userProfileSettingsResponse;
import service.clientProfileService;
import service.lawyerProifileService;

@RestController
@RequestMapping("/api/user/settings")
public class userSettingsController {
    private final clientProfileService ClientProfileService;
    private final lawyerProifileService LawyerProfileService;
    private final service.authService AuthService;

    public userSettingsController(clientProfileService ClientProfileService,
            lawyerProifileService LawyerProfileService, service.authService AuthService) {
        this.ClientProfileService = ClientProfileService;
        this.LawyerProfileService = LawyerProfileService;
        this.AuthService = AuthService;
    }

    @org.springframework.web.bind.annotation.PostMapping("/request-change-otp")
    public apiResponse<Void> requestChangeOTP(@RequestBody dto.ChangeRequestDTO request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AuthService.requestProfileChangeOTP(auth.getName(), request.getType(), request.getNewValue());
        return new apiResponse<>("Verification OTP sent", true, null);
    }

    @PutMapping("/change-password")
    public apiResponse<Void> changePassword(@RequestBody dto.ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AuthService.changePassword(auth.getName(), request.getOldPassword(), request.getNewPassword());
        return new apiResponse<>("Password updated successfully", true, null);
    }

    @PutMapping("/preferences")
    public apiResponse<Void> updatePreferences(@RequestBody dto.NotificationPreferencesRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AuthService.updatePreferences(auth.getName(), request);
        return new apiResponse<>("Preferences updated successfully", true, null);
    }

    @GetMapping()
    public apiResponse<userProfileSettingsResponse> getMySettings() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        userProfileSettingsResponse resp;

        // Logic to determine which service to call based on User Roles
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LAWYER"))) {
            resp = LawyerProfileService.getSettingsByEmail(email);
        } else {
            resp = ClientProfileService.getSettingsByEmail(email);
        }

        return new apiResponse<>("Profile settings fetched", true, resp);
    }

    @PutMapping()
    public apiResponse<userProfileSettingsResponse> updateMySettings(@RequestBody userProfileSettingsRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        userProfileSettingsResponse resp;

        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LAWYER"))) {
            resp = LawyerProfileService.updateSettingsByEmail(email, request);
        } else {
            resp = ClientProfileService.updateSettingsByEmail(email, request);
        }

        return new apiResponse<>("Profile settings updated", true, resp);
    }
}
