package dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Data;

@Data
public class SubscriptionResponse {
    private Long id;
    private String userId;
    private int price;
    private Long contactView;
    private String type;
    private int duration;
    private LocalDate startDate;
    private LocalDate endDate;
    private String subscriptionId;
    private String planType;
    private String status;
    private String cancellationType;
    private LocalDateTime cancelledAt;
    private LocalDateTime effectiveCancellationDate;
    private String cancellationReason;
    private Double refundAmount;
    private Boolean refundProcessed;
    private LocalDateTime refundProcessedAt;
    private String refundId;
    private Integer remainingDays;
    private String shortUrl;
    private String message;
    private boolean success;
}
