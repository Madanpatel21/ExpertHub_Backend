package controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.razorpay.RazorpayException;

import dto.SubscriptionCancellationRequest;
import dto.SubscriptionRequest;
import dto.SubscriptionResponse;
import dto.apiResponse;
import entity.user;
import exception.badRequestException;
import service.SubscriptionService;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * Create a new subscription
     * POST /api/subscriptions/create
     */
    @PostMapping("/create")
    public ResponseEntity<apiResponse<SubscriptionResponse>> createSubscription(
            @RequestBody SubscriptionRequest request,
            @AuthenticationPrincipal user currentUser) {

        try {
            if (!request.getUserId().equals(currentUser.getId())) {
                return ResponseEntity.badRequest()
                        .body(new apiResponse<>("User ID mismatch", false, null));
            }

            SubscriptionResponse response = subscriptionService.createSubscription(request);
            return ResponseEntity.ok(new apiResponse<>("Subscription created", true, response));

        } catch (RazorpayException e) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>("Failed to create subscription: " + e.getMessage(), false, null));
        }
    }

    /**
     * Cancel a subscription
     * POST /api/subscriptions/cancel
     */
    @PostMapping("/cancel")
    public ResponseEntity<apiResponse<SubscriptionResponse>> cancelSubscription(
            @RequestBody SubscriptionCancellationRequest request,
            @AuthenticationPrincipal user currentUser) {

        try {
            if (!request.getUserId().equals(currentUser.getId())) {
                return ResponseEntity.badRequest()
                        .body(new apiResponse<>("User ID mismatch", false, null));
            }

            SubscriptionResponse response = subscriptionService.cancelSubscription(request);
            return ResponseEntity.ok(new apiResponse<>("Subscription cancelled", true, response));

        } catch (badRequestException e) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>(e.getMessage(), false, null));
        }
    }

    /**
     * Cancel subscription by ID
     * POST /api/subscriptions/{subscriptionId}/cancel
     */
    @PostMapping("/{subscriptionId}/cancel")
    public ResponseEntity<apiResponse<SubscriptionResponse>> cancelSubscriptionById(
            @PathVariable Long subscriptionId,
            @RequestBody(required = false) SubscriptionCancellationRequest request,
            @AuthenticationPrincipal user currentUser) {

        try {
            SubscriptionCancellationRequest cancelRequest = new SubscriptionCancellationRequest();
            cancelRequest.setSubscriptionId(subscriptionId);
            cancelRequest.setUserId(currentUser.getId());

            if (request != null) {
                cancelRequest.setCancelImmediately(request.isCancelImmediately());
                cancelRequest.setRequestRefund(request.isRequestRefund());
                cancelRequest.setReason(request.getReason());
            } else {
                // Default: cancel immediately with no refund
                cancelRequest.setCancelImmediately(true);
                cancelRequest.setRequestRefund(false);
                cancelRequest.setReason("User requested cancellation");
            }

            SubscriptionResponse response = subscriptionService.cancelSubscription(cancelRequest);
            return ResponseEntity.ok(new apiResponse<>("Subscription cancelled", true, response));

        } catch (badRequestException e) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>(e.getMessage(), false, null));
        }
    }

    /**
     * Request refund for a subscription
     * POST /api/subscriptions/{subscriptionId}/refund
     */
    @PostMapping("/{subscriptionId}/refund")
    public ResponseEntity<apiResponse<SubscriptionResponse>> requestRefund(
            @PathVariable Long subscriptionId,
            @RequestBody(required = false) SubscriptionCancellationRequest request,
            @AuthenticationPrincipal user currentUser) {

        try {
            String reason = request != null && request.getReason() != null
                    ? request.getReason()
                    : "User requested refund";

            SubscriptionResponse response = subscriptionService.processRefund(
                    subscriptionId,
                    currentUser.getId().toString(),
                    reason);
            return ResponseEntity.ok(new apiResponse<>("Refund processed", true, response));

        } catch (badRequestException e) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>(e.getMessage(), false, null));
        }
    }

    /**
     * Get subscription by ID
     * GET /api/subscriptions/{subscriptionId}
     */
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<apiResponse<SubscriptionResponse>> getSubscription(
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal user currentUser) {

        try {
            SubscriptionResponse response = subscriptionService.getSubscription(
                    subscriptionId,
                    currentUser.getId().toString());
            return ResponseEntity.ok(new apiResponse<>("Subscription retrieved", true, response));

        } catch (badRequestException e) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>(e.getMessage(), false, null));
        }
    }

    /**
     * Get all active subscriptions for current user
     * GET /api/subscriptions/active
     */
    @GetMapping("/active")
    public ResponseEntity<apiResponse<List<SubscriptionResponse>>> getActiveSubscriptions(
            @AuthenticationPrincipal user currentUser) {

        List<SubscriptionResponse> subscriptions = subscriptionService.getActiveSubscriptions(
                currentUser.getId().toString());
        return ResponseEntity.ok(new apiResponse<>("Active subscriptions retrieved", true, subscriptions));
    }

    /**
     * Get all subscriptions for current user
     * GET /api/subscriptions
     */
    @GetMapping
    public ResponseEntity<apiResponse<List<SubscriptionResponse>>> getAllSubscriptions(
            @AuthenticationPrincipal user currentUser) {

        List<SubscriptionResponse> subscriptions = subscriptionService.getAllSubscriptions(
                currentUser.getId().toString());
        return ResponseEntity.ok(new apiResponse<>("All subscriptions retrieved", true, subscriptions));
    }

    /**
     * Check if user has active subscription
     * GET /api/subscriptions/check-active
     */
    @GetMapping("/check-active")
    public ResponseEntity<apiResponse<Boolean>> checkActiveSubscription(
            @AuthenticationPrincipal user currentUser) {

        boolean hasActive = subscriptionService.hasActiveSubscription(currentUser.getId().toString());
        return ResponseEntity.ok(new apiResponse<>("Active subscription check", true, hasActive));
    }

    /**
     * Delete/Cancel subscription by ID (alternative endpoint)
     * DELETE /api/subscriptions/{subscriptionId}
     */
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<apiResponse<SubscriptionResponse>> deleteSubscription(
            @PathVariable Long subscriptionId,
            @RequestBody(required = false) SubscriptionCancellationRequest request,
            @AuthenticationPrincipal user currentUser) {

        try {
            SubscriptionCancellationRequest cancelRequest = new SubscriptionCancellationRequest();
            cancelRequest.setSubscriptionId(subscriptionId);
            cancelRequest.setUserId(currentUser.getId());
            cancelRequest.setCancelImmediately(true);
            cancelRequest.setRequestRefund(false);
            cancelRequest.setReason(request != null && request.getReason() != null
                    ? request.getReason()
                    : "User deleted subscription");

            SubscriptionResponse response = subscriptionService.cancelSubscription(cancelRequest);
            return ResponseEntity.ok(new apiResponse<>("Subscription deleted", true, response));

        } catch (badRequestException e) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>(e.getMessage(), false, null));
        }
    }
}
