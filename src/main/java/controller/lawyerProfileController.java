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

import Repository.clientProfileRepository;
import Repository.lawyerProfileRepository;
import dto.apiResponse;
import dto.lawyerProfileRequest;
import entity.lawyerProfile;
import entity.user;
import exception.badRequestException;

import org.springframework.web.bind.annotation.RequestBody;
import service.lawyerProifileService;
import service.profileImageService;

@RestController
@RequestMapping("/api/lawyer/profile")
public class lawyerProfileController {
    private final lawyerProifileService LawyerProfileService;
    private final profileImageService ProfileImageService;
    private final lawyerProfileRepository lawyerRepo;

    public lawyerProfileController(lawyerProifileService LawyerProfileService, profileImageService ProfileImageService,
            lawyerProfileRepository lawyerRepo) {
        this.LawyerProfileService = LawyerProfileService;
        this.ProfileImageService = ProfileImageService;
        this.lawyerRepo = lawyerRepo;

    }

    @PostMapping("/{userId}")
    public apiResponse<String> createProfile(@PathVariable Long userId, @RequestBody lawyerProfileRequest request) {
        LawyerProfileService.createProfile(userId, request);

        return new apiResponse<>("Lawyer profile created", true, null);
    }

    @GetMapping("/{profileId}")
    public apiResponse<lawyerProfile> getLawyerProfile(@PathVariable Long profileId) {
        lawyerProfile profile = LawyerProfileService.getProfileById(profileId);

        return new apiResponse<>("Lawyer profile fetched successfully", true, profile);
    }

    @PostMapping("/{userId}/upload-image")
    public apiResponse<String> uploadLawyerProfileImage(@PathVariable Long userId,
            @RequestParam("image") MultipartFile image) {

        user User = new user();
        User.setId(userId);
        lawyerProfile profile = lawyerRepo.findByUser(User)
                .orElseThrow(() -> new badRequestException("Lawyer profile not found"));

        String filename = ProfileImageService.saveProfileImage(image);
        profile.setProfileImagePath(filename);
        lawyerRepo.save(profile);
        return new apiResponse<>("Lawyer Image Uploaded Successfully", true, filename);
    }

    @PutMapping("/{userId}/update-image")
    public apiResponse<String> updateProfileImage(@PathVariable Long userId,
            @RequestParam("image") MultipartFile image) {
        user User = new user();
        User.setId(userId);
        lawyerProfile profile = lawyerRepo.findByUser(User)
                .orElseThrow(() -> new badRequestException("Lawyer profile not found"));
        String existing = profile.getProfileImagePath();
        String file = ProfileImageService.updateProfileImage(image, existing);
        profile.setProfileImagePath(file);
        lawyerRepo.save(profile);
        return new apiResponse<>("Profile Image Updated", true, file);
    }

    @DeleteMapping("/{userId}/delete-image")
    public apiResponse<Boolean> deleteProfileImage(@PathVariable Long userId) {
        user User = new user();
        User.setId(userId);
        lawyerProfile profile = lawyerRepo.findByUser(User)
                .orElseThrow(() -> new badRequestException("Lawyer profile not found"));
        String existing = profile.getProfileImagePath();
        boolean deleted = ProfileImageService.deleteProfileImage(existing);
        if (deleted) {
            profile.setProfileImagePath(null);
            lawyerRepo.save(profile);
        }
        return new apiResponse<>(deleted ? "Profile Image Deleted" : "No image to delete", true, deleted);
    }

    @PostMapping("/{userId}/upload-verification-doc")
    public apiResponse<String> uploadVerificationDoc(@PathVariable Long userId,
            @RequestParam("document") MultipartFile document) {

        user User = new user();
        User.setId(userId);
        lawyerProfile profile = lawyerRepo.findByUser(User)
                .orElseThrow(() -> new badRequestException("Lawyer profile not found"));

        String filename = ProfileImageService.saveProfileImage(document);
        profile.setVerificationDocumentPath(filename);
        lawyerRepo.save(profile);
        return new apiResponse<>("Verification Document Uploaded Successfully", true, filename);
    }

    @PostMapping("/{userId}/upload-verification-video")
    public apiResponse<String> uploadVerificationVideo(@PathVariable Long userId,
            @RequestParam("video") MultipartFile video) {

        user User = new user();
        User.setId(userId);
        lawyerProfile profile = lawyerRepo.findByUser(User)
                .orElseThrow(() -> new badRequestException("Lawyer profile not found"));

        String filename = ProfileImageService.saveProfileImage(video);
        profile.setVerificationVideoPath(filename);
        lawyerRepo.save(profile);
        return new apiResponse<>("Verification Video Uploaded Successfully", true, filename);
    }

}
