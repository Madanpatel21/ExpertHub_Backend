package dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundResponse {
    private Long refundId;
    private String razorpayRefundId;
    private Long appointmentId;
    private String paymentId;
    private String status;
    private Double refundAmount;
    private String reason;
    private String refundedBy;
    private String refundedAt;
    private String message;
}
