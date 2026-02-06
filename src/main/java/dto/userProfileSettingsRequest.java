package dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class userProfileSettingsRequest {
    private String displayName;
    private String phone;
    private String location;
    private String bio;
    private Boolean publicProfile;
    private String email;
    private String otp;
}
