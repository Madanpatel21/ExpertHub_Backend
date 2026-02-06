package dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ReviewRequest {
    private Long appointmentId;
    private Long lawyerId;

    @Min(1)
    @Max(5)
    private int rating;

    private String comment;

}
