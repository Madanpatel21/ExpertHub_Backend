package dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppointmentRequestDTO {
    private Long lawyerId;
    private LocalDateTime dateTime;
}
