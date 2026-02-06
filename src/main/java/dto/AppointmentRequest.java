package dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppointmentRequest {

    private Long lawyerId;
    private LocalDateTime dateTime;

}
