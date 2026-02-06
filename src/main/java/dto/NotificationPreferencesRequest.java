package dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationPreferencesRequest {
    private Boolean pushEnabled;
    private Boolean emailEnabled;
}
