package dto;

import lombok.Getter;

import lombok.Setter;

@Getter
@Setter
public class registerRequest {
    private String name;
    private String Email;
    private String Password;
    private String phoneNumber;
    private String Otp;
    private String Role;
}
