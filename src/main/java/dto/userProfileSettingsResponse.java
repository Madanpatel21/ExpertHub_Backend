package dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class userProfileSettingsResponse {
    private String email;
    private String displayName;
    private String phone;
    private String location;
    private String bio;
    private Boolean publicProfile;
    private String profileImagePath;
}
