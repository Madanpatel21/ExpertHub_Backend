package entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class lawyerProfile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String fullName;
	@Column(nullable = false, unique = true)
	private String barCouncilNumber;
	@Column(nullable = false)
	private int yearsOfExperience;
	@Column(nullable = false)
	private String practiceArea;
	@Column(nullable = false)
	private String city;
	@Column(nullable = false)
	private double consultationFees;
	private boolean verified;
	@OneToOne
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private user user;

	private String profileImagePath;

	private Integer totalReviews;
	private Double averageRating;

	private String verificationDocumentPath;
	private String verificationVideoPath;
	@Column(name = "verification_status", nullable = false)
	private String verificationStatus = "PENDING";

	// Advanced Discovery Fields
	private Double latitude;
	private Double longitude;
	private Boolean onlineStatus = false;
	private java.time.LocalDateTime lastSeen;

}
