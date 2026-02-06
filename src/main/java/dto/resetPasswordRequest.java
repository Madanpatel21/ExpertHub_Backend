package dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class resetPasswordRequest {
    
    @NotBlank(message = "Token cannot be blank")
    private String token;
    
    @NotBlank(message = "New password cannot be blank")
    private String newPassword;
    
    @NotBlank(message = "Confirm password cannot be blank")
    private String confirmPassword;
}
