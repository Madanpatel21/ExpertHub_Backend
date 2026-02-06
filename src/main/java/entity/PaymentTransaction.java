package entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String idempotencyKey;
    private Long appointmentId;
    private String userId;
    private Long amount;
    private String currency;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String failureReason;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime lastRetryAt;
    private Integer retryCount = 0;

    // Refund fields
    private String razorpayRefundId;
    private Float refundAmount;
    private String refundReason;
    private String refundedBy; // User ID who initiated refund
    private LocalDateTime refundedAt;

    public enum PaymentStatus {
        PENDING,
        SUCCESS,
        FAILED,
        CANCELLED,
        REFUNDED
    }

    public boolean canRetry() {
        return status == PaymentStatus.FAILED && retryCount < 3;
    }
}
