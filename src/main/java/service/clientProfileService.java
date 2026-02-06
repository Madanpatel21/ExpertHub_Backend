package service;

import org.springframework.stereotype.Service;

import Repository.clientProfileRepository;
import Repository.userRepository;
import dto.clientProfileRequest;
import dto.userProfileSettingsRequest;
import dto.userProfileSettingsResponse;
import entity.clientProfile;
import entity.user;
import exception.badRequestException;

@Service
public class clientProfileService {
	private final clientProfileRepository ClientProfileRepository;
	private final userRepository UserRepository;
	private final authService AuthService;

	public clientProfileService(clientProfileRepository ClientProfileRepository,
			userRepository UserRepository, authService AuthService) {
		this.ClientProfileRepository = ClientProfileRepository;
		this.UserRepository = UserRepository;
		this.AuthService = AuthService;
	}

	public void createProfile(Long userId, clientProfileRequest request) {
		user User = UserRepository.findById(userId).orElseThrow(() -> new badRequestException("User not found"));
		clientProfile profile = new clientProfile();
		profile.setFullName(request.getFullName());
		profile.setPhone(request.getPhone());
		profile.setCity(request.getCity());
		profile.setUser(User);

		ClientProfileRepository.save(profile);
	}

	public void updateProfileImage(Long profileId, String fileName) {
		clientProfile profile = ClientProfileRepository.findById(profileId)
				.orElseThrow(() -> new badRequestException("Client profile Not found"));
		profile.setProfileImagePath(fileName);
		ClientProfileRepository.save(profile);
	}

	public userProfileSettingsResponse getSettingsByEmail(String email) {
		user User = UserRepository.findByEmail(email)
				.orElseThrow(() -> new badRequestException("User not found"));

		clientProfile profile = ClientProfileRepository.findByUser(User)
				.orElseThrow(() -> new badRequestException("Client profile not found"));

		// Returning the 7-parameter DTO
		return new userProfileSettingsResponse(
				User.getEmail(), // 1. email
				profile.getFullName(), // 2. displayName
				profile.getPhone(), // 3. phone (Clients have this!)
				profile.getCity(), // 4. location
				null, // 5. bio (placeholder)
				true, // 6. publicProfile
				profile.getProfileImagePath() // 7. image
		);
	}

	public userProfileSettingsResponse updateSettingsByEmail(String email, userProfileSettingsRequest request) {
		user User = UserRepository.findByEmail(email)
				.orElseThrow(() -> new badRequestException("User not found"));

		clientProfile profile = ClientProfileRepository.findByUser(User)
				.orElseThrow(() -> new badRequestException("Client profile not found"));

		// 🛡️ SECURE UPDATES: Check if Phone or Email is changing
		boolean phoneChanged = request.getPhone() != null && !request.getPhone().equals(profile.getPhone());
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
				profile.setPhone(request.getPhone());
				User.setPhoneNumber(request.getPhone()); // Sync with User entity
			}

			if (emailChanged) {
				if (!AuthService.verifyProfileChangeOTP(email, dto.ChangeRequestDTO.ChangeType.EMAIL,
						request.getOtp())) {
					throw new badRequestException("Invalid or expired OTP for email change");
				}
				User.setEmail(request.getEmail());
			}
		}

		// Update other fields
		profile.setFullName(request.getDisplayName());
		profile.setCity(request.getLocation());

		UserRepository.save(User);
		ClientProfileRepository.save(profile);

		return getSettingsByEmail(User.getEmail());
	}
}
