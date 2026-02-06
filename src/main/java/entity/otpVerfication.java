package entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class otpVerfication {
	
	 	@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    private String email;

	    private String phoneNumber;

	    private String otpHash;

	    private LocalDateTime expiryTime;

	    private int attempts;
	    
	    private LocalDateTime lastOtpSentAt;

	    private boolean verified;
}
