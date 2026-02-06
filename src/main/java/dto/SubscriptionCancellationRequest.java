package dto;

import lombok.Data;

@Data
public class SubscriptionCancellationRequest {
    private Long subscriptionId;
    private Long userId; // Changed from String to Long
    private boolean cancelImmediately;
    private boolean requestRefund;
    private String reason;
}
