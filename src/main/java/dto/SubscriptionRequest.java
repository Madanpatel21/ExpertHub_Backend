package dto;

import java.util.Map;

import lombok.Data;

@Data
public class SubscriptionRequest {
    private Long userId; // Changed from String to Long
    private String planId;
    private String customerId;
    private Integer totalCount;
    private String couponCode;
    private Map<String, String> notes;
}
