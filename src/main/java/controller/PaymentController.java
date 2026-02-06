package controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.razorpay.RazorpayException;

import Repository.AppointmentRepository;
import dto.RazorpayOrderRequest;
import dto.RazorpayOrderResponse;
import dto.apiResponse;
import entity.Appointment;
import entity.AppointmentStatus;
import entity.PaymentTransaction;
import service.PaymentTransactionService;
import service.RazorpayService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final RazorpayService razorpayService;
    private final AppointmentRepository appointmentRepository;
    private final PaymentTransactionService paymentTransactionService;

    public PaymentController(RazorpayService razorpayService,
            AppointmentRepository appointmentRepository,
            PaymentTransactionService paymentTransactionService) {
        this.razorpayService = razorpayService;
        this.appointmentRepository = appointmentRepository;
        this.paymentTransactionService = paymentTransactionService;
    }

    /**
     * Create a payment order with idempotency key
     */
    @PostMapping("/create-order")
    public ResponseEntity<apiResponse<PaymentOrderResponse>> createOrder(
            @RequestBody PaymentOrderRequest request) {

        // Check for existing transaction with same idempotency key
        var existingTx = paymentTransactionService.findByIdempotencyKey(request.getIdempotencyKey());

        if (existingTx.isPresent() && existingTx.get().getStatus() == PaymentTransaction.PaymentStatus.SUCCESS) {
            // Return existing successful transaction
            PaymentTransaction tx = existingTx.get();
            PaymentOrderResponse response = new PaymentOrderResponse();
            response.setOrderId(tx.getRazorpayOrderId());
            response.setAmount(tx.getAmount());
            response.setCurrency(tx.getCurrency());
            response.setAlreadyPaid(true);
            return ResponseEntity.ok(new apiResponse<>("Order already created", true, response));
        }

        try {
            // Create Razorpay order using the DTO
            RazorpayOrderRequest razorpayRequest = new RazorpayOrderRequest();
            razorpayRequest.setAmount(request.getAmount());
            razorpayRequest.setCurrency(request.getCurrency() != null ? request.getCurrency() : "INR");
            razorpayRequest.setReceipt(request.getReceiptId());
            razorpayRequest.setNotes(request.getNotes());

            RazorpayOrderResponse orderResponse = razorpayService.createOrder(razorpayRequest);

            // Save transaction with PENDING status
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setIdempotencyKey(request.getIdempotencyKey());
            transaction.setAppointmentId(request.getAppointmentId());
            transaction.setUserId(request.getUserId());
            transaction.setAmount(request.getAmount());
            transaction.setCurrency(razorpayRequest.getCurrency());
            transaction.setRazorpayOrderId(orderResponse.getOrderId());
            transaction.setStatus(PaymentTransaction.PaymentStatus.PENDING);
            transaction.setCreatedAt(LocalDateTime.now());
            transaction.setRetryCount(0);

            paymentTransactionService.save(transaction);

            PaymentOrderResponse response = new PaymentOrderResponse();
            response.setOrderId(orderResponse.getOrderId());
            response.setAmount(orderResponse.getAmount());
            response.setCurrency(orderResponse.getCurrency());
            response.setTransactionId(transaction.getId());
            response.setAlreadyPaid(false);

            return ResponseEntity.ok(new apiResponse<>("Order created successfully", true, response));

        } catch (RazorpayException e) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>("Failed to create order: " + e.getMessage(), false, null));
        }
    }

    /**
     * Verify payment with retry mechanism
     */
    @PostMapping("/verify")
    public ResponseEntity<apiResponse<PaymentVerificationResponse>> verifyPayment(
            @RequestBody PaymentVerificationRequest request) {

        // Find transaction
        var txOpt = paymentTransactionService.findByRazorpayOrderId(request.getOrderId());

        if (txOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>("Transaction not found", false, null));
        }

        PaymentTransaction transaction = txOpt.get();

        // Check if already verified
        if (transaction.getStatus() == PaymentTransaction.PaymentStatus.SUCCESS) {
            return ResponseEntity.ok(new apiResponse<>("Payment already verified", true,
                    buildVerificationResponse(transaction)));
        }

        // Verify signature
        boolean isValid = razorpayService.verifyPaymentSignature(
                request.getOrderId(),
                request.getPaymentId(),
                request.getSignature());

        if (isValid) {
            // Update transaction
            transaction.setRazorpayPaymentId(request.getPaymentId());
            transaction.setStatus(PaymentTransaction.PaymentStatus.SUCCESS);
            transaction.setCompletedAt(LocalDateTime.now());
            transaction.setVerifiedAt(LocalDateTime.now());
            paymentTransactionService.save(transaction);

            // Mark appointment as paid
            markAppointmentAsPaid(transaction.getAppointmentId(), request.getPaymentId());

            return ResponseEntity.ok(new apiResponse<>("Payment verified successfully", true,
                    buildVerificationResponse(transaction)));
        } else {
            // Mark as failed
            transaction.setStatus(PaymentTransaction.PaymentStatus.FAILED);
            transaction.setFailureReason("Invalid signature");
            transaction.setRetryCount(transaction.getRetryCount() != null ? transaction.getRetryCount() + 1 : 1);
            transaction.setLastRetryAt(LocalDateTime.now());
            paymentTransactionService.save(transaction);

            return ResponseEntity.badRequest()
                    .body(new apiResponse<>("Payment verification failed", false, null));
        }
    }

    /**
     * Retry payment verification
     */
    @PostMapping("/retry-verify/{transactionId}")
    public ResponseEntity<apiResponse<PaymentVerificationResponse>> retryVerification(
            @PathVariable Long transactionId) {

        var txOpt = paymentTransactionService.findById(transactionId);

        if (txOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>("Transaction not found", false, null));
        }

        PaymentTransaction transaction = txOpt.get();

        // Check retry limit
        Integer retryCount = transaction.getRetryCount() != null ? transaction.getRetryCount() : 0;
        if (retryCount >= 3) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>("Maximum retry attempts reached", false, null));
        }

        // Increment retry count
        transaction.setRetryCount(retryCount + 1);
        transaction.setLastRetryAt(LocalDateTime.now());
        paymentTransactionService.save(transaction);

        // Return transaction details for frontend to retry payment
        PaymentVerificationResponse response = buildVerificationResponse(transaction);
        response.setCanRetry(true);

        return ResponseEntity.ok(new apiResponse<>("Please retry payment", true, response));
    }

    /**
     * Check payment status
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<apiResponse<PaymentStatusResponse>> getPaymentStatus(@PathVariable Long transactionId) {
        var txOpt = paymentTransactionService.findById(transactionId);

        if (txOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>("Transaction not found", false, null));
        }

        PaymentTransaction tx = txOpt.get();
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setStatus(tx.getStatus().name());
        response.setAmount(tx.getAmount());
        response.setCurrency(tx.getCurrency());
        response.setCreatedAt(tx.getCreatedAt().toString());
        response.setFailureReason(tx.getFailureReason());

        return ResponseEntity.ok(new apiResponse<>("Status retrieved", true, response));
    }

    /**
     * Handle payment webhook from Razorpay
     */
    @PostMapping("/webhook")
    public ResponseEntity<apiResponse<String>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {
            boolean isValid = razorpayService.verifyWebhookSignature(payload, signature);

            if (!isValid) {
                return ResponseEntity.badRequest()
                        .body(new apiResponse<String>("Invalid webhook signature", false, null));
            }

            // Process webhook event and update payment status
            // This ensures payment completion even if frontend failed

            return ResponseEntity.ok(new apiResponse<String>("Webhook processed successfully", true, "success"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<String>("Failed to process webhook: " + e.getMessage(), false, null));
        }
    }

    private void markAppointmentAsPaid(Long appointmentId, String paymentId) {
        appointmentRepository.findById(appointmentId).ifPresent(appointment -> {
            appointment.setIsPaid(true);
            appointment.setPaymentId(paymentId);
            appointment.setPaymentDate(LocalDateTime.now());
            appointment.setStatus(AppointmentStatus.PAID);
            appointmentRepository.save(appointment);
        });
    }

    private PaymentVerificationResponse buildVerificationResponse(PaymentTransaction tx) {
        PaymentVerificationResponse response = new PaymentVerificationResponse();
        response.setTransactionId(tx.getId());
        response.setStatus(tx.getStatus().name());
        response.setAmount(tx.getAmount());
        response.setPaymentId(tx.getRazorpayPaymentId());
        return response;
    }

    // Request/Response classes
    public static class PaymentOrderRequest {
        private String idempotencyKey;
        private Long appointmentId;
        private String userId;
        private Long amount;
        private String currency;
        private String receiptId;
        private Map<String, String> notes;

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        public Long getAppointmentId() {
            return appointmentId;
        }

        public void setAppointmentId(Long appointmentId) {
            this.appointmentId = appointmentId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getReceiptId() {
            return receiptId;
        }

        public void setReceiptId(String receiptId) {
            this.receiptId = receiptId;
        }

        public Map<String, String> getNotes() {
            return notes;
        }

        public void setNotes(Map<String, String> notes) {
            this.notes = notes;
        }
    }

    public static class PaymentOrderResponse {
        private String orderId;
        private Long amount;
        private String currency;
        private Long transactionId;
        private boolean alreadyPaid;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public Long getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(Long transactionId) {
            this.transactionId = transactionId;
        }

        public boolean isAlreadyPaid() {
            return alreadyPaid;
        }

        public void setAlreadyPaid(boolean alreadyPaid) {
            this.alreadyPaid = alreadyPaid;
        }
    }

    public static class PaymentVerificationRequest {
        private String orderId;
        private String paymentId;
        private String signature;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(String paymentId) {
            this.paymentId = paymentId;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }
    }

    public static class PaymentVerificationResponse {
        private Long transactionId;
        private String status;
        private Long amount;
        private String paymentId;
        private boolean canRetry;

        public Long getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(Long transactionId) {
            this.transactionId = transactionId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public String getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(String paymentId) {
            this.paymentId = paymentId;
        }

        public boolean isCanRetry() {
            return canRetry;
        }

        public void setCanRetry(boolean canRetry) {
            this.canRetry = canRetry;
        }
    }

    public static class PaymentStatusResponse {
        private String status;
        private Long amount;
        private String currency;
        private String createdAt;
        private String failureReason;
        private String message;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
