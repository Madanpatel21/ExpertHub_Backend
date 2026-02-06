package service;

import org.springframework.stereotype.Service;

import Repository.userRepository;
import dto.userProfileSettingsRequest;
import dto.userProfileSettingsResponse;
import entity.user;
import exception.badRequestException;

@Service
public class userProfileService {
    private final userRepository userRepo;

    public userProfileService(userRepository userRepo) {
        this.userRepo = userRepo;
    }

    public userProfileSettingsResponse getSettingsByEmail(String email) {
        user User = userRepo.findByEmail(email).orElseThrow(() -> new badRequestException("User not found"));
        return mapToResponse(User);
    }

    public userProfileSettingsResponse updateSettingsByEmail(String email, userProfileSettingsRequest req) {
        user User = userRepo.findByEmail(email).orElseThrow(() -> new badRequestException("User not found"));
        if (req.getDisplayName() != null)
            User.setName(req.getDisplayName());
        if (req.getPhone() != null)
            User.setPhoneNumber(req.getPhone());
        if (req.getLocation() != null)
            User.setLocation(req.getLocation());
        if (req.getBio() != null)
            User.setBio(req.getBio());
        if (req.getPublicProfile() != null)
            User.setPublicProfile(req.getPublicProfile());
        userRepo.save(User);
        return mapToResponse(User);
    }

    private userProfileSettingsResponse mapToResponse(user User) {
        userProfileSettingsResponse response = new userProfileSettingsResponse();
        response.setEmail(User.getEmail());
        response.setDisplayName(User.getName());
        response.setPhone(User.getPhoneNumber());
        response.setLocation(User.getLocation());
        response.setBio(User.getBio());
        response.setPublicProfile(User.getPublicProfile());
        response.setProfileImagePath(User.getProfileImagePath());
        return response;
    }
}
