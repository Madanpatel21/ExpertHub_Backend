package entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity 
@Table(name = "registration")
@Data
public class Registration {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long  id;
	
	private String  userId;
	private String username;
	private String phoneNumber;
	private String password;
	private String confirmPassword;
	private String createProfileFor;
	private String gender;
	private String referCode;
	private String fcmToken;
	
    private String roles = "ROLE_USER";
}
