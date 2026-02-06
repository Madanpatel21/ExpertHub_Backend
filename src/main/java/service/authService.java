package service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Repository.roleRepository;
import Repository.userRepository;
import Util.JwtUtil;
import dto.loginRequest;
import dto.otpVerifyRequest;
import dto.registerRequest;
import entity.roles;
import entity.user;
import exception.badRequestException;

@Service
public class authService {
	@Autowired
	private final userRepository userRepo;
	@Autowired
	private final roleRepository roleRepo;
	private final JwtUtil jwtUtil;
	private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;
	private final RedisOTPService redisOtpService;
	private final RateLimitService rateLimitService;
	private final otpService OtpService;

	public authService(userRepository userRepo, roleRepository roleRepo,
			JwtUtil jwtUtil, org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder,
			RedisOTPService redisOtpService, RateLimitService rateLimitService,
			otpService OtpService) {
		this.userRepo = userRepo;
		this.roleRepo = roleRepo;
		this.jwtUtil = jwtUtil;
		this.passwordEncoder = passwordEncoder;
		this.redisOtpService = redisOtpService;
		this.rateLimitService = rateLimitService;
		this.OtpService = OtpService;
	}

	// Register
	public void register(registerRequest request) {
		if (userRepo.existsByEmail(request.getEmail())) {
			throw new badRequestException("Email Already Registered");
		}

		roles Role = roleRepo.findByName(request.getRole()).orElseThrow(() -> new badRequestException("Invalid Role"));
		user User = new user();
		User.setEmail(request.getEmail());
		User.setPassword(passwordEncoder.encode(request.getPassword()));
		User.setName(request.getName());
		User.setPhoneNumber(request.getPhoneNumber());
		User.setRole(Role);

		userRepo.save(User);

	}

	// Login
	public void login(loginRequest request) {
		user User = null;

		if (request.getEmail() != null && !request.getEmail().isEmpty()) {
			User = userRepo.findByEmail(request.getEmail())
					.orElseThrow(() -> new badRequestException("User Doesn't Exists"));
		} else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
			User = userRepo.findByPhoneNumber(request.getPhoneNumber())
					.orElseThrow(() -> new badRequestException("User Doesn't Exists"));
		} else {
			throw new badRequestException("Email or Phone Number is required");
		}

		// Rate Limit Check (3 OTPs per 10 mins)
		if (!rateLimitService.isAllowed("login_otp", User.getPhoneNumber(), 3, 10)) {
			throw new badRequestException("Too many OTP requests. Please wait 10 minutes.");
		}

		if (!passwordEncoder.matches(request.getPassword(), User.getPassword())) {
			throw new badRequestException("Invalid Password");
		}

		String otp = redisOtpService.generateAndStoreOTP("login", User.getPhoneNumber());

		// 📱 SEND THE OTP via SMS (MSG91/Twilio)
		try {
			OtpService.sendSMSText(User.getPhoneNumber(), "Your Expert Hub login OTP is: " + otp);
		} catch (Exception e) {
			System.err.println("Failed to send SMS: " + e.getMessage());
		}

		System.out.println("Login OTP (Redis) : " + otp);
	}

	public String verifyOtp(otpVerifyRequest request) {
		user User = userRepo.findByPhoneNumber(request.getPhoneNumber())
				.orElseThrow(() -> new badRequestException("User not found"));

		if (!redisOtpService.validateOTP("login", User.getPhoneNumber(), request.getOtp())) {
			throw new badRequestException("Invalid or Expired OTP ");
		}

		String roleName = User.getRole() != null ? User.getRole().getName() : "USER";
		return jwtUtil.generateToken(User.getEmail(), roleName);

	}

	public java.util.Date getJwtExpiry(String token) {
		return jwtUtil.getExpiryFromToken(token);
	}

	public String getEmailFromPhone(String phoneNumber) {
		user User = userRepo.findByPhoneNumber(phoneNumber)
				.orElseThrow(() -> new badRequestException("User not found"));
		return User.getEmail();
	}

	@Autowired
	private Repository.clientProfileRepository clientRepo;
	@Autowired
	private Repository.lawyerProfileRepository lawyerRepo;
	@Autowired
	private profileImageService ProfileImageService;

	public void deleteAccount(Long userId) {
		user User = userRepo.findById(userId).orElseThrow(() -> new badRequestException("User not found"));

		// 1. Delete media and profile based on role
		if (User.getRole().getName().equals("LAWYER")) {
			lawyerRepo.findByUser(User).ifPresent(profile -> {
				// Delete all lawyer media
				ProfileImageService.deleteProfileImage(profile.getProfileImagePath());
				ProfileImageService.deleteProfileImage(profile.getVerificationDocumentPath());
				ProfileImageService.deleteProfileImage(profile.getVerificationVideoPath());
				lawyerRepo.delete(profile);
			});
		} else if (User.getRole().getName().equals("CLIENT")) {
			clientRepo.findByUser(User).ifPresent(profile -> {
				ProfileImageService.deleteProfileImage(profile.getProfileImagePath());
				clientRepo.delete(profile);
			});
		}

		// 2. Delete user-level media
		ProfileImageService.deleteProfileImage(User.getProfileImagePath());

		// 3. Delete user
		userRepo.delete(User);
	}

	public void changePassword(String email, String oldPassword, String newPassword) {
		user User = userRepo.findByEmail(email).orElseThrow(() -> new badRequestException("User not found"));
		if (!passwordEncoder.matches(oldPassword, User.getPassword())) {
			throw new badRequestException("Current password incorrect");
		}
		User.setPassword(passwordEncoder.encode(newPassword));
		userRepo.save(User);
	}

	@Autowired
	private EmailService emailService;

	public void requestProfileChangeOTP(String currentEmail, dto.ChangeRequestDTO.ChangeType type, String newValue) {
		// Rate limit check
		if (!rateLimitService.isAllowed("profile_change_otp", currentEmail, 3, 15)) {
			throw new badRequestException("Too many requests. Please wait 15 minutes.");
		}

		String otp = redisOtpService.generateAndStoreOTP("profile_change:" + type.name(), currentEmail);

		if (type == dto.ChangeRequestDTO.ChangeType.EMAIL) {
			emailService.sendSimpleMessage(newValue, "Expert Hub: Email Change Verification",
					"Your verification code to change your email is: " + otp);
		} else {
			OtpService.sendSMSText(newValue, "Your Expert Hub phone change OTP is: " + otp);
		}

		System.out.println("Profile Change OTP for " + currentEmail + " (" + type + ") : " + otp);
	}

	public boolean verifyProfileChangeOTP(String currentEmail, dto.ChangeRequestDTO.ChangeType type, String otp) {
		return redisOtpService.validateOTP("profile_change:" + type.name(), currentEmail, otp);
	}

	public void updatePreferences(String email, dto.NotificationPreferencesRequest request) {
		user User = userRepo.findByEmail(email).orElseThrow(() -> new badRequestException("User not found"));
		// Add preference update logic here
		userRepo.save(User);
	}
}
