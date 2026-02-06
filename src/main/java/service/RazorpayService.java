package service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.razorpay.Subscription;

import dto.RazorpayOrderRequest;
import dto.RazorpayOrderResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Service
public class RazorpayService {

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    public RazorpayService(@Value("${razorpay.key.id}") String keyId,
            @Value("${razorpay.key.secret}") String keySecret) throws RazorpayException {
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
    }

    public RazorpayOrderResponse createOrder(RazorpayOrderRequest request) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", request.getAmount());
        orderRequest.put("currency", request.getCurrency());
        orderRequest.put("receipt", request.getReceipt());

        if (request.getNotes() != null) {
            orderRequest.put("notes", new JSONObject(request.getNotes()));
        }

        Order order = razorpayClient.orders.create(orderRequest);

        RazorpayOrderResponse response = new RazorpayOrderResponse();
        response.setOrderId(order.get("id"));
        response.setAmount(order.get("amount"));
        response.setCurrency(order.get("currency"));
        response.setStatus(order.get("status"));

        return response;
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            String expectedSignature = computeSignature(payload, keySecret);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            String expectedSignature = computeSignature(payload, webhookSecret);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private String computeSignature(String data, String secret) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("sha256");
        String input = data + secret;
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String getKeyId() {
        return keyId;
    }

    // ========== Subscription Methods (compatible with Razorpay SDK 1.4.x)
    // ==========

    /**
     * Create a subscription in Razorpay
     */
    public Map<String, Object> createSubscription(String planId, String customerId, int totalCount,
            Map<String, String> notes) throws RazorpayException {
        JSONObject subscriptionRequest = new JSONObject();
        subscriptionRequest.put("plan_id", planId);
        subscriptionRequest.put("customer_id", customerId);
        subscriptionRequest.put("total_count", totalCount);
        subscriptionRequest.put("quantity", 1);

        if (notes != null) {
            subscriptionRequest.put("notes", new JSONObject(notes));
        }

        Subscription subscription = razorpayClient.subscriptions.create(subscriptionRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("id", subscription.get("id"));
        response.put("status", subscription.get("status"));
        response.put("start_at", subscription.get("start_at"));
        response.put("end_at", subscription.get("end_at"));
        response.put("charge_at", subscription.get("charge_at"));
        response.put("short_url", subscription.get("short_url"));

        return response;
    }

    /**
     * Cancel a subscription
     */
    public Map<String, Object> cancelSubscription(String subscriptionId, boolean cancelAtCycleEnd)
            throws RazorpayException {
        JSONObject cancelRequest = new JSONObject();

        if (cancelAtCycleEnd) {
            cancelRequest.put("cancel_at_cycle_end", true);
        } else {
            cancelRequest.put("cancel_at_cycle_end", false);
        }

        Subscription subscription = razorpayClient.subscriptions.cancel(subscriptionId, cancelRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("id", subscription.get("id"));
        response.put("status", subscription.get("status"));
        response.put("cancel_at_cycle_end", subscription.get("cancel_at_cycle_end"));
        response.put("ended_at", subscription.get("ended_at"));

        return response;
    }

    /**
     * Pause a subscription
     */
    public Map<String, Object> pauseSubscription(String subscriptionId) throws RazorpayException {
        JSONObject pauseRequest = new JSONObject();

        Subscription subscription = razorpayClient.subscriptions.pause(subscriptionId, pauseRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("id", subscription.get("id"));
        response.put("status", subscription.get("status"));
        response.put("pause_at", subscription.get("pause_at"));

        return response;
    }

    /**
     * Resume a paused subscription
     */
    public Map<String, Object> resumeSubscription(String subscriptionId) throws RazorpayException {
        JSONObject resumeRequest = new JSONObject();

        Subscription subscription = razorpayClient.subscriptions.resume(subscriptionId, resumeRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("id", subscription.get("id"));
        response.put("status", subscription.get("status"));
        response.put("resume_at", subscription.get("resume_at"));

        return response;
    }

    /**
     * Get subscription details
     */
    public Map<String, Object> getSubscription(String subscriptionId) throws RazorpayException {
        Subscription subscription = razorpayClient.subscriptions.fetch(subscriptionId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", subscription.get("id"));
        response.put("status", subscription.get("status"));
        response.put("customer_id", subscription.get("customer_id"));
        response.put("plan_id", subscription.get("plan_id"));
        response.put("start_at", subscription.get("start_at"));
        response.put("end_at", subscription.get("end_at"));
        response.put("charge_at", subscription.get("charge_at"));
        response.put("cancel_at_cycle_end", subscription.get("cancel_at_cycle_end"));
        response.put("remaining_count", subscription.get("remaining_count"));
        response.put("paid_count", subscription.get("paid_count"));

        return response;
    }

    // ========== Refund Methods (compatible with Razorpay SDK 1.4.x) ==========

    /**
     * Create a refund for a payment
     */
    public Map<String, Object> createRefund(String paymentId, Integer amount, Map<String, String> notes)
            throws RazorpayException {
        JSONObject refundRequest = new JSONObject();

        if (amount != null) {
            refundRequest.put("amount", amount);
        }

        refundRequest.put("payment_id", paymentId);

        if (notes != null) {
            refundRequest.put("notes", new JSONObject(notes));
        }

        Refund refund = razorpayClient.refunds.create(refundRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("id", refund.get("id"));
        response.put("payment_id", refund.get("payment_id"));
        response.put("amount", refund.get("amount"));
        response.put("status", refund.get("status"));
        response.put("created_at", refund.get("created_at"));
        response.put("speed", refund.get("speed"));

        return response;
    }

    /**
     * Get refund details
     */
    public Map<String, Object> getRefund(String refundId) throws RazorpayException {
        Refund refund = razorpayClient.refunds.fetch(refundId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", refund.get("id"));
        response.put("payment_id", refund.get("payment_id"));
        response.put("amount", refund.get("amount"));
        response.put("status", refund.get("status"));
        response.put("created_at", refund.get("created_at"));
        response.put("speed", refund.get("speed"));
        response.put("receipt", refund.get("receipt"));

        return response;
    }

    /**
     * Get payment details (to check if refund is possible)
     */
    public Map<String, Object> getPayment(String paymentId) throws RazorpayException {
        Payment payment = razorpayClient.payments.fetch(paymentId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", payment.get("id"));
        response.put("status", payment.get("status"));
        response.put("amount", payment.get("amount"));
        response.put("currency", payment.get("currency"));
        response.put("refunded_at", payment.get("refunded_at"));
        response.put("refund_status", payment.get("refund_status"));

        return response;
    }

    /**
     * Calculate prorated refund amount
     */
    public int calculateProratedRefund(int totalAmount, int daysRemaining, int daysUsed) {
        if (daysRemaining <= 0) {
            return 0;
        }

        double dailyRate = (double) totalAmount / (daysRemaining + daysUsed);
        return (int) Math.round(dailyRate * daysRemaining);
    }
}
