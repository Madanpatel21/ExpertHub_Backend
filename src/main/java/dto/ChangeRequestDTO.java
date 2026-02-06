package dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChangeRequestDTO {
    public enum ChangeType {
        EMAIL, PHONE
    }

    private ChangeType type;
    private String newValue;
}
