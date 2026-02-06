package service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.razorpay.RazorpayException;

import Repository.SubscriberRepo;
import dto.SubscriptionCancellationRequest;
import dto.SubscriptionRequest;
import dto.SubscriptionResponse;
import entity.Subscriber;
import entity.Subscriber.CancellationType;
import entity.Subscriber.SubscriptionStatus;
import exception.ResourceNotFoundException;
import exception.badRequestException;

@Service
public class SubscriptionService {

    private final SubscriberRepo subscriberRepo;
    private final RazorpayService razorpayService;

    public SubscriptionService(SubscriberRepo subscriberRepo, RazorpayService razorpayService) {
        this.subscriberRepo = subscriberRepo;
        this.razorpayService = razorpayService;
    }

    /**
     * Create a new subscription
     */
    @Transactional
    public SubscriptionResponse createSubscription(SubscriptionRequest request) throws RazorpayException {
        Map<String, String> notes = new HashMap<>();
        notes.put("userId", request.getUserId().toString());
        if (request.getCouponCode() != null) {
            notes.put("couponCode", request.getCouponCode());
        }

        Map<String, Object> razorpayResponse = razorpayService.createSubscription(
                request.getPlanId(),
                request.getCustomerId(),
                request.getTotalCount() != null ? request.getTotalCount() : 12,
                notes);

        Subscriber subscriber = new Subscriber();
        subscriber.setUserId(request.getUserId());
        subscriber.setSubscriptionId((String) razorpayResponse.get("id"));
        subscriber.setStatus(SubscriptionStatus.ACTIVE);
        subscriber.setPlanType(request.getPlanId());
        subscriber.setCreatedAt(LocalDateTime.now());
        subscriber.setUpdatedAt(LocalDateTime.now());

        // Set subscription period
        LocalDate startDate = LocalDate.now();
        subscriber.setStartDate(startDate);
        subscriber.setStartTime(LocalTime.now());

        // Calculate end date based on duration
        int duration = request.getTotalCount() != null ? request.getTotalCount() : 12;
        subscriber.setEndDate(startDate.plusMonths(duration));
        subscriber.setEndTime(LocalTime.now());

        subscriber = subscriberRepo.save(subscriber);

        SubscriptionResponse response = mapToResponse(subscriber);
        response.setShortUrl((String) razorpayResponse.get("short_url"));
        response.setMessage("Subscription created successfully");
        return response;
    }

    /**
     * Cancel a subscription
     * Can be immediate or end of billing period
     * Can include refund if requested
     */
    @Transactional
    public SubscriptionResponse cancelSubscription(SubscriptionCancellationRequest request) {
        Subscriber subscriber = subscriberRepo.findById(request.getSubscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        // Verify ownership
        if (!subscriber.getUserId().equals(request.getUserId())) {
            throw new badRequestException("You can only cancel your own subscription");
        }

        // Check if already cancelled
        if (subscriber.getStatus() == SubscriptionStatus.CANCELLED ||
                subscriber.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new badRequestException("Subscription is already cancelled or expired");
        }

        try {
            if (subscriber.getSubscriptionId() != null) {
                // Cancel in Razorpay
                Map<String, Object> cancelResponse = razorpayService.cancelSubscription(
                        subscriber.getSubscriptionId(),
                        !request.isCancelImmediately());

                if (request.isCancelImmediately()) {
                    // Immediate cancellation
                    subscriber.setStatus(SubscriptionStatus.CANCELLED);
                    subscriber.setEffectiveCancellationDate(LocalDateTime.now());

                    // Process refund if requested
                    if (request.isRequestRefund()) {
                        processRefund(subscriber, request.getReason());
                    }
                } else {
                    // Cancel at end of billing period
                    subscriber.setStatus(SubscriptionStatus.CANCELLED);
                    subscriber.setCancellationType(CancellationType.END_OF_BILLING_PERIOD);
                    subscriber.setEffectiveCancellationDate(subscriber.getEndDate().atTime(subscriber.getEndTime()));
                }
            } else {
                // No Razorpay subscription - just cancel locally
                if (request.isCancelImmediately()) {
                    subscriber.setStatus(SubscriptionStatus.CANCELLED);
                    subscriber.setEffectiveCancellationDate(LocalDateTime.now());

                    if (request.isRequestRefund()) {
                        processRefund(subscriber, request.getReason());
                    }
                } else {
                    subscriber.setStatus(SubscriptionStatus.CANCELLED);
                    subscriber.setCancellationType(CancellationType.END_OF_BILLING_PERIOD);
                    subscriber.setEffectiveCancellationDate(subscriber.getEndDate().atTime(subscriber.getEndTime()));
                }
            }

            subscriber.setCancelledAt(LocalDateTime.now());
            subscriber.setCancellationReason(request.getReason());
            subscriber.setUpdatedAt(LocalDateTime.now());

            subscriber = subscriberRepo.save(subscriber);

            SubscriptionResponse response = mapToResponse(subscriber);
            response.setMessage("Subscription cancelled successfully");
            return response;

        } catch (RazorpayException e) {
            throw new badRequestException("Failed to cancel subscription: " + e.getMessage());
        }
    }

    /**
     * Process refund for a subscription
     */
    @Transactional
    public SubscriptionResponse processRefund(Long subscriptionId, String userId, String reason) {
        Subscriber subscriber = subscriberRepo.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        if (!subscriber.getUserId().toString().equals(userId)) {
            throw new badRequestException("You can only request refund for your own subscription");
        }

        if (subscriber.getStatus() == SubscriptionStatus.REFUNDED) {
            throw new badRequestException("Subscription has already been refunded");
        }

        processRefund(subscriber, reason);

        subscriber = subscriberRepo.save(subscriber);

        SubscriptionResponse response = mapToResponse(subscriber);
        response.setMessage("Refund processed successfully");
        return response;
    }

    /**
     * Internal method to process refund
     */
    private void processRefund(Subscriber subscriber, String reason) {
        try {
            if (subscriber.getRazorpayPaymentId() == null) {
                // No payment ID - mark as pending refund
                subscriber.setStatus(SubscriptionStatus.PENDING_REFUND);
                subscriber.setRefundReason(reason);
                return;
            }

            // Calculate refund amount
            Integer refundAmount = calculateRefundAmount(subscriber);

            // Create refund in Razorpay
            Map<String, String> notes = new HashMap<>();
            notes.put("subscriptionId", subscriber.getSubscriptionId());
            notes.put("reason", reason);

            Map<String, Object> refundResponse = razorpayService.createRefund(
                    subscriber.getRazorpayPaymentId(),
                    refundAmount,
                    notes);

            subscriber.setRefundId((String) refundResponse.get("id"));
            subscriber.setRefundAmount(refundAmount != null ? refundAmount.doubleValue() : 0.0);
            subscriber.setRefundProcessed(true);
            subscriber.setRefundProcessedAt(LocalDateTime.now());
            subscriber.setRefundReason(reason);
            subscriber.setStatus(SubscriptionStatus.REFUNDED);
            subscriber.setUpdatedAt(LocalDateTime.now());

        } catch (RazorpayException e) {
            throw new badRequestException("Failed to process refund: " + e.getMessage());
        }
    }

    /**
     * Calculate prorated refund amount
     */
    private Integer calculateRefundAmount(Subscriber subscriber) {
        // Calculate days remaining
        LocalDate endDate = subscriber.getEndDate();
        LocalDate startDate = subscriber.getStartDate();

        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        long daysUsed = java.time.temporal.ChronoUnit.DAYS.between(startDate, LocalDate.now());
        long daysRemaining = totalDays - daysUsed;

        if (daysRemaining <= 0) {
            return 0;
        }

        return razorpayService.calculateProratedRefund(
                subscriber.getPrice(),
                (int) daysRemaining,
                (int) daysUsed);
    }

    /**
     * Get subscription by ID
     */
    public SubscriptionResponse getSubscription(Long subscriptionId, String userId) {
        Subscriber subscriber = subscriberRepo.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        if (!subscriber.getUserId().toString().equals(userId)) {
            throw new badRequestException("You can only view your own subscription");
        }

        return mapToResponse(subscriber);
    }

    /**
     * Get all active subscriptions for a user
     */
    public List<SubscriptionResponse> getActiveSubscriptions(String userId) {
        Long userIdLong = Long.parseLong(userId);
        List<Subscriber> subscribers = subscriberRepo.findActiveSubscriptionsByUserId(userIdLong);
        return subscribers.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get all subscriptions for a user
     */
    public List<SubscriptionResponse> getAllSubscriptions(String userId) {
        Long userIdLong = Long.parseLong(userId);
        List<Subscriber> subscribers = subscriberRepo.findAll();
        return subscribers.stream()
                .filter(s -> s.getUserId().equals(userIdLong))
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Check if user has active subscription
     */
    public boolean hasActiveSubscription(String userId) {
        Long userIdLong = Long.parseLong(userId);
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        return subscriberRepo.existsByUserIdAndEndDateAfterAndEndTimeAfterMain(userIdLong, currentDate, currentTime);
    }

    /**
     * Update subscription status to expired if end date passed
     */
    @Transactional
    public void checkAndExpireSubscriptions() {
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        List<Subscriber> expired = subscriberRepo.findAll();
        for (Subscriber subscriber : expired) {
            if (subscriber.getStatus() == SubscriptionStatus.ACTIVE &&
                    subscriber.getEndDate() != null &&
                    subscriber.getEndDate().isBefore(currentDate)) {
                subscriber.setStatus(SubscriptionStatus.EXPIRED);
                subscriber.setUpdatedAt(LocalDateTime.now());
                subscriberRepo.save(subscriber);
            }
        }
    }

    /**
     * Map Subscriber entity to SubscriptionResponse DTO
     */
    private SubscriptionResponse mapToResponse(Subscriber subscriber) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setId(subscriber.getId());
        response.setUserId(subscriber.getUserId() != null ? subscriber.getUserId().toString() : null);
        response.setPrice(subscriber.getPrice());
        response.setContactView(subscriber.getContactView());
        response.setType(subscriber.getType());
        response.setDuration(subscriber.getDuration());
        response.setStartDate(subscriber.getStartDate());
        response.setEndDate(subscriber.getEndDate());
        response.setSubscriptionId(subscriber.getSubscriptionId());
        response.setPlanType(subscriber.getPlanType());
        response.setStatus(subscriber.getStatus() != null ? subscriber.getStatus().name() : null);
        response.setCancellationType(
                subscriber.getCancellationType() != null ? subscriber.getCancellationType().name() : null);
        response.setCancelledAt(subscriber.getCancelledAt());
        response.setEffectiveCancellationDate(subscriber.getEffectiveCancellationDate());
        response.setCancellationReason(subscriber.getCancellationReason());
        response.setRefundAmount(subscriber.getRefundAmount());
        response.setRefundProcessed(subscriber.getRefundProcessed());
        response.setRefundProcessedAt(subscriber.getRefundProcessedAt());
        response.setRefundId(subscriber.getRefundId());

        // Calculate remaining days
        if (subscriber.getEndDate() != null && subscriber.getStatus() != SubscriptionStatus.EXPIRED) {
            long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), subscriber.getEndDate());
            response.setRemainingDays((int) Math.max(0, remainingDays));
        }

        response.setSuccess(true);
        return response;
    }
}
