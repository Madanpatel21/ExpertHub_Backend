package controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import Repository.clientProfileRepository;
import dto.apiResponse;
import dto.clientProfileRequest;
import entity.clientProfile;
import entity.user;
import exception.badRequestException;
import org.springframework.web.bind.annotation.RequestBody;
import service.clientProfileService;
import service.profileImageService;

@RestController
@RequestMapping("/api/client/profile")
public class clientProfileController {
	private final clientProfileService ClientProfileService;
	private final profileImageService ProfileImageService;
	private final clientProfileRepository clientRepo;

	public clientProfileController(clientProfileService ClientProfileService,
			profileImageService ProfileImageService,
			clientProfileRepository clientRepo) {
		this.ClientProfileService = ClientProfileService;
		this.ProfileImageService = ProfileImageService;
		this.clientRepo = clientRepo;
	}

	@PostMapping("/{userId}")
	public apiResponse<String> createProfile(@PathVariable Long userId, @RequestBody clientProfileRequest request) {
		ClientProfileService.createProfile(userId, request);
		return new apiResponse<>("Client profile created", true, null);
	}

	@PostMapping("/{userId}/upload-image")
	public apiResponse<String> uploadProfileImage(
			@PathVariable Long userId, @RequestParam("image") MultipartFile image) {

		user User = new user();
		User.setId(userId);
		clientProfile profile = clientRepo.findByUser(User)
				.orElseThrow(() -> new badRequestException("Client profile not found"));

		String file = ProfileImageService.saveProfileImage(image);
		profile.setProfileImagePath(file);
		clientRepo.save(profile);

		return new apiResponse<>("Profile Image Uploaded", true, file);
	}

	@PutMapping("/{userId}/update-image")
	public apiResponse<String> updateProfileImage(@PathVariable Long userId,
			@RequestParam("image") MultipartFile image) {
		user User = new user();
		User.setId(userId);
		clientProfile profile = clientRepo.findByUser(User)
				.orElseThrow(() -> new badRequestException("Client profile not found"));
		String existing = profile.getProfileImagePath();
		String file = ProfileImageService.updateProfileImage(image, existing);
		profile.setProfileImagePath(file);
		clientRepo.save(profile);
		return new apiResponse<>("Profile Image Updated", true, file);
	}

	@DeleteMapping("/{userId}/delete-image")
	public apiResponse<Boolean> deleteProfileImage(@PathVariable Long userId) {
		user User = new user();
		User.setId(userId);
		clientProfile profile = clientRepo.findByUser(User)
				.orElseThrow(() -> new badRequestException("Client profile not found"));
		String existing = profile.getProfileImagePath();
		boolean deleted = ProfileImageService.deleteProfileImage(existing);
		if (deleted) {
			profile.setProfileImagePath(null);
			clientRepo.save(profile);
		}
		return new apiResponse<>(deleted ? "Profile Image Deleted" : "No image to delete", true, deleted);
	}
}
