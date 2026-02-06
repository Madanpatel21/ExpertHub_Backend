package dto;

import lombok.Getter;

import lombok.Setter;

@Getter
@Setter
public class loginRequest {
	private String email;
	private String PhoneNumber;
	private String Password;
	private String Otp;
}
