package entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
@Table(name = "subscriber")
public class Subscriber {

    public enum SubscriptionStatus {
        ACTIVE,
        CANCELLED,
        EXPIRED,
        PENDING_REFUND,
        REFUNDED
    }

    public enum CancellationType {
        IMMEDIATE,
        END_OF_BILLING_PERIOD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // Changed from String to Long to match user entity
    private int price;
    private Long contactView;
    private String type;
    private int duration;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String eventType;
    private String subscriptionId; // Razorpay subscription ID
    private String cuponCode;
    private Integer remainsDays;
    private String description;
    private String planType;

    // Cancellation fields
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    private CancellationType cancellationType;

    private LocalDateTime cancelledAt;
    private LocalDateTime effectiveCancellationDate;
    private String cancellationReason;

    // Refund fields
    private Double refundAmount;
    private Boolean refundProcessed = false;
    private LocalDateTime refundProcessedAt;
    private String refundId; // Razorpay refund ID
    private String refundReason;

    // Razorpay payment ID for original subscription payment
    private String razorpayPaymentId;
    private String razorpayOrderId;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
