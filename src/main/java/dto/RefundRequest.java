package dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundRequest {
    private Long appointmentId;
    private String reason;
    private Boolean fullRefund; // true = full refund, false = partial
    private Long partialAmount; // if partial refund
}
