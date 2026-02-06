package service;

import org.springframework.stereotype.Service;

import Repository.lawyerProfileRepository;
import Repository.userRepository;
import dto.lawyerProfileRequest;
import dto.userProfileSettingsRequest;
import dto.userProfileSettingsResponse;
import entity.lawyerProfile;
import entity.user;
import exception.badRequestException;

@Service
public class lawyerProifileService {
    private final lawyerProfileRepository LawyerProfileRepository;
    private final userRepository UserRepository;
    private final authService AuthService;

    public lawyerProifileService(lawyerProfileRepository LawyerProfileRepository, userRepository UserRepository,
            authService AuthService) {
        this.LawyerProfileRepository = LawyerProfileRepository;
        this.UserRepository = UserRepository;
        this.AuthService = AuthService;
    }

    public void createProfile(Long userId, lawyerProfileRequest request) {

        user User = UserRepository.findById(userId)
                .orElseThrow(() -> new badRequestException("User not found"));
        if (!User.getRole().getName().equals("LAWYER")) {
            throw new badRequestException("User is not a lawyer");
        }
        if (LawyerProfileRepository.existsByUser(User)) {
            throw new badRequestException("Lawyer profile already exists");
        }
        lawyerProfile profile = new lawyerProfile();
        profile.setFullName(request.getFullName());
        profile.setBarCouncilNumber(request.getBarCouncilNumber());
        profile.setYearsOfExperience(request.getYearsOfExperience());
        profile.setPracticeArea(request.getPracticeArea());
        profile.setCity(request.getCity());
        profile.setConsultationFees(request.getConsultationFees());
        profile.setVerified(false);
        profile.setUser(User);

        LawyerProfileRepository.save(profile);
    }

    public lawyerProfile getProfileById(Long profileId) {

        return LawyerProfileRepository.findById(profileId)
                .orElseThrow(() -> new badRequestException("Lawyer profile not found"));
    }

    public void updateProfileImage(Long profileId, String fileName) {
        lawyerProfile profile = LawyerProfileRepository.findById(profileId)
                .orElseThrow(() -> new badRequestException("Lawyer profile not found"));
        profile.setProfileImagePath(fileName);
        LawyerProfileRepository.save(profile);

    }

    public userProfileSettingsResponse getSettingsByEmail(String email) {
        user User = UserRepository.findByEmail(email)
                .orElseThrow(() -> new badRequestException("User not found"));

        lawyerProfile profile = LawyerProfileRepository.findByUser(User)
                .orElseThrow(() -> new badRequestException("Lawyer profile not found"));

        return new userProfileSettingsResponse(
                User.getEmail(), // email
                profile.getFullName(), // displayName (from fullName)
                null, // phone (lawyerProfile doesn't have a phone field yet)
                profile.getCity(), // location (from city)
                profile.getPracticeArea(), // bio (using practiceArea as bio)
                profile.isVerified(), // publicProfile (using verified status)
                profile.getProfileImagePath()// Assuming these fields exist in your DTO
        );
    }

    public userProfileSettingsResponse updateSettingsByEmail(String email, userProfileSettingsRequest request) {
        user User = UserRepository.findByEmail(email)
                .orElseThrow(() -> new badRequestException("User not found"));

        lawyerProfile profile = LawyerProfileRepository.findByUser(User)
                .orElseThrow(() -> new badRequestException("Lawyer profile not found"));

        // 🛡️ SECURE UPDATES: Check if Phone or Email is changing
        // (Note: Lawyers store phone in the 'user' entity, not 'lawyerProfile')
        boolean phoneChanged = request.getPhone() != null && !request.getPhone().equals(User.getPhoneNumber());
        boolean emailChanged = request.getEmail() != null && !request.getEmail().equals(User.getEmail());

        if (phoneChanged || emailChanged) {
            if (request.getOtp() == null || request.getOtp().isEmpty()) {
                throw new badRequestException("OTP required for email/phone change");
            }

            if (phoneChanged) {
                if (!AuthService.verifyProfileChangeOTP(email, dto.ChangeRequestDTO.ChangeType.PHONE,
                        request.getOtp())) {
                    throw new badRequestException("Invalid or expired OTP for phone change");
                }
                User.setPhoneNumber(request.getPhone());
            }

            if (emailChanged) {
                if (!AuthService.verifyProfileChangeOTP(email, dto.ChangeRequestDTO.ChangeType.EMAIL,
                        request.getOtp())) {
                    throw new badRequestException("Invalid or expired OTP for email change");
                }
                User.setEmail(request.getEmail());
            }
        }

        profile.setFullName(request.getDisplayName());
        profile.setCity(request.getLocation());
        // bio/practiceArea update
        if (request.getBio() != null) {
            profile.setPracticeArea(request.getBio());
        }

        UserRepository.save(User);
        LawyerProfileRepository.save(profile);

        return getSettingsByEmail(User.getEmail());
    }

    public void updateVerificationDocument(Long profileId, String fileName) {
        lawyerProfile profile = LawyerProfileRepository.findById(profileId)
                .orElseThrow(() -> new badRequestException("Lawyer profile not found"));
        profile.setVerificationDocumentPath(fileName);
        LawyerProfileRepository.save(profile);
    }

    public void updateVerificationVideo(Long profileId, String fileName) {
        lawyerProfile profile = LawyerProfileRepository.findById(profileId)
                .orElseThrow(() -> new badRequestException("Lawyer profile not found"));
        profile.setVerificationVideoPath(fileName);
        LawyerProfileRepository.save(profile);
    }

}
