package controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import Repository.userRepository;
import dto.apiResponse;
import entity.user;
import exception.badRequestException;
import service.profileImageService;

@RestController
@RequestMapping("/api/user/profile")
public class userProfileController {
    private final userRepository userRepo;
    private final profileImageService ProfileImageService;
    private final service.authService AuthService;
    private final service.UserStatusService StatusService;

    public userProfileController(userRepository userRepo, profileImageService ProfileImageService,
            service.authService AuthService, service.UserStatusService StatusService) {
        this.userRepo = userRepo;
        this.ProfileImageService = ProfileImageService;
        this.AuthService = AuthService;
        this.StatusService = StatusService;
    }

    @PostMapping("/{userId}/heartbeat")
    public apiResponse<Void> heartbeat(@PathVariable String userId) {
        StatusService.heartbeat(userId);
        return new apiResponse<>("Heartbeat received", true, null);
    }

    @GetMapping("/{userId}/online-status")
    public apiResponse<Boolean> getOnlineStatus(@PathVariable String userId) {
        boolean online = StatusService.isUserOnline(userId);
        return new apiResponse<>("Status fetched", true, online);
    }

    @DeleteMapping("/{userId}")
    public apiResponse<Void> deleteAccount(@PathVariable Long userId) {
        AuthService.deleteAccount(userId);
        return new apiResponse<>("Account Deleted Successfully", true, null);
    }

    @PostMapping("/{userId}/upload-image")
    public apiResponse<String> uploadProfileImage(@PathVariable Long userId,
            @RequestParam("image") MultipartFile image) {
        user User = userRepo.findById(userId).orElseThrow(() -> new badRequestException("User not found"));
        String file = ProfileImageService.saveProfileImage(image);
        User.setProfileImagePath(file);
        userRepo.save(User);
        return new apiResponse<>("Profile Image Uploaded", true, file);
    }

    @PutMapping("/{userId}/update-image")
    public apiResponse<String> updateProfileImage(@PathVariable Long userId,
            @RequestParam("image") MultipartFile image) {
        user User = userRepo.findById(userId).orElseThrow(() -> new badRequestException("User not found"));
        String existing = User.getProfileImagePath();
        String file = ProfileImageService.updateProfileImage(image, existing);
        User.setProfileImagePath(file);
        userRepo.save(User);
        return new apiResponse<>("Profile Image Updated", true, file);
    }

    @DeleteMapping("/{userId}/delete-image")
    public apiResponse<Boolean> deleteProfileImage(@PathVariable Long userId) {
        user User = userRepo.findById(userId).orElseThrow(() -> new badRequestException("User not found"));
        String existing = User.getProfileImagePath();
        boolean deleted = ProfileImageService.deleteProfileImage(existing);
        if (deleted) {
            User.setProfileImagePath(null);
            userRepo.save(User);
        }
        return new apiResponse<>(deleted ? "Profile Image Deleted" : "No image to delete", true, deleted);
    }
}
