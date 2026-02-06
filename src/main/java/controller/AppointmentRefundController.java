package controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.razorpay.RazorpayException;

import Repository.AppointmentRepository;
import Repository.PaymentTransactionRepo;
import dto.RefundRequest;
import dto.RefundResponse;
import dto.apiResponse;
import entity.Appointment;
import entity.AppointmentStatus;
import entity.PaymentTransaction;
import exception.UnauthorizedException;
import exception.badRequestException;
import service.RazorpayService;

@RestController
@RequestMapping("/api")
public class AppointmentRefundController {

    private final PaymentTransactionRepo paymentTransactionRepo;
    private final AppointmentRepository appointmentRepository;
    private final RazorpayService razorpayService;

    public AppointmentRefundController(PaymentTransactionRepo paymentTransactionRepo,
            AppointmentRepository appointmentRepository,
            RazorpayService razorpayService) {
        this.paymentTransactionRepo = paymentTransactionRepo;
        this.appointmentRepository = appointmentRepository;
        this.razorpayService = razorpayService;
    }

    /**
     * Get authenticated user ID
     */
    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }
        return auth.getName();
    }

    /**
     * Check if user is admin
     */
    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Lawyer initiates refund for consultation fee
     * POST /api/appointments/{appointmentId}/refund
     */
    @PostMapping("/appointments/{appointmentId}/refund")
    @PreAuthorize("hasRole('LAWYER')")
    public ResponseEntity<apiResponse<RefundResponse>> lawyerInitiateRefund(
            @PathVariable Long appointmentId,
            @RequestBody RefundRequest request) {

        try {
            // Get authenticated lawyer ID
            String userId = getAuthenticatedUserId();

            // Find appointment
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new badRequestException("Appointment not found"));

            // Verify the lawyer owns this appointment
            if (!appointment.getLawyerId().toString().equals(userId)) {
                throw new UnauthorizedException("You can only refund appointments assigned to you");
            }

            // Process refund
            RefundResponse response = processRefund(appointment, request.getReason(), userId);

            return ResponseEntity.ok(new apiResponse<>("Refund initiated successfully", true, response));

        } catch (badRequestException e) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>(e.getMessage(), false, null));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(401)
                    .body(new apiResponse<>(e.getMessage(), false, null));
        } catch (RazorpayException e) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>("Refund failed: " + e.getMessage(), false, null));
        }
    }

    /**
     * Admin initiates refund for consultation fee
     * POST /api/admin/appointments/{appointmentId}/refund
     */
    @PostMapping("/admin/appointments/{appointmentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<apiResponse<RefundResponse>> adminInitiateRefund(
            @PathVariable Long appointmentId,
            @RequestBody RefundRequest request) {

        try {
            // Find appointment
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new badRequestException("Appointment not found"));

            // Process refund as admin
            String adminId = getAuthenticatedUserId();
            RefundResponse response = processRefund(appointment, request.getReason(), adminId);

            return ResponseEntity.ok(new apiResponse<>("Refund initiated by admin", true, response));

        } catch (badRequestException e) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>(e.getMessage(), false, null));
        } catch (RazorpayException e) {
            return ResponseEntity.badRequest()
                    .body(new apiResponse<>("Refund failed: " + e.getMessage(), false, null));
        }
    }

    /**
     * Get refund status for an appointment
     * GET /api/appointments/{appointmentId}/refund-status
     */
    @GetMapping("/appointments/{appointmentId}/refund-status")
    public ResponseEntity<apiResponse<RefundResponse>> getRefundStatus(@PathVariable Long appointmentId) {

        // Find appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new badRequestException("Appointment not found"));

        // Find payment transaction
        Optional<PaymentTransaction> txOpt = paymentTransactionRepo.findByAppointmentIdAndStatus(
                appointmentId, PaymentTransaction.PaymentStatus.REFUNDED);

        if (txOpt.isEmpty()) {
            return ResponseEntity.ok(new apiResponse<>("No refund found", true,
                    new RefundResponse(null, null, appointmentId, null, "NOT_REFUNDED",
                            0.0, null, null, null, "No refund has been processed")));
        }

        PaymentTransaction tx = txOpt.get();

        RefundResponse response = new RefundResponse();
        response.setAppointmentId(appointmentId);
        response.setPaymentId(tx.getRazorpayPaymentId());
        response.setRazorpayRefundId(tx.getRazorpayRefundId());
        response.setStatus("REFUNDED");
        response.setRefundAmount(tx.getRefundAmount() != null ? tx.getRefundAmount().doubleValue() : 0.0);
        response.setReason(tx.getRefundReason());
        response.setRefundedBy(tx.getRefundedBy());
        response.setRefundedAt(tx.getRefundedAt() != null ? tx.getRefundedAt().toString() : null);
        response.setMessage("Refund processed successfully");

        return ResponseEntity.ok(new apiResponse<>("Refund status retrieved", true, response));
    }

    /**
     * Process refund logic
     */
    private RefundResponse processRefund(Appointment appointment, String reason, String refundedBy)
            throws RazorpayException, badRequestException {

        // Check if appointment is paid
        if (!Boolean.TRUE.equals(appointment.getIsPaid())) {
            throw new badRequestException("Appointment has not been paid. Cannot refund.");
        }

        // Find the successful payment transaction
        Optional<PaymentTransaction> txOpt = paymentTransactionRepo.findByAppointmentIdAndStatus(
                appointment.getId(), PaymentTransaction.PaymentStatus.SUCCESS);

        if (txOpt.isEmpty()) {
            throw new badRequestException("No successful payment found for this appointment");
        }

        PaymentTransaction tx = txOpt.get();

        // Check if already refunded
        if (tx.getStatus() == PaymentTransaction.PaymentStatus.REFUNDED) {
            throw new badRequestException("This appointment has already been refunded");
        }

        // Get payment ID
        String paymentId = tx.getRazorpayPaymentId();
        if (paymentId == null || paymentId.isEmpty()) {
            throw new badRequestException("No payment ID found for this transaction");
        }

        // Calculate refund amount (full refund)
        int refundAmount = tx.getAmount().intValue();

        // Create refund in Razorpay
        Map<String, String> notes = new HashMap<>();
        notes.put("appointment_id", appointment.getId().toString());
        notes.put("refund_reason", reason);
        notes.put("refunded_by", refundedBy);

        Map<String, Object> refundResult = razorpayService.createRefund(paymentId, refundAmount, notes);

        // Update transaction
        tx.setStatus(PaymentTransaction.PaymentStatus.REFUNDED);
        tx.setRazorpayRefundId((String) refundResult.get("id"));
        tx.setRefundAmount((float) refundAmount);
        tx.setRefundReason(reason);
        tx.setRefundedBy(refundedBy);
        tx.setRefundedAt(LocalDateTime.now());
        paymentTransactionRepo.save(tx);

        // Update appointment status to REFUNDED
        appointment.setStatus(AppointmentStatus.REFUNDED);
        appointmentRepository.save(appointment);

        // Build response
        RefundResponse response = new RefundResponse();
        response.setRefundId(tx.getId());
        response.setRazorpayRefundId((String) refundResult.get("id"));
        response.setAppointmentId(appointment.getId());
        response.setPaymentId(paymentId);
        response.setStatus("REFUNDED");
        response.setRefundAmount((double) refundAmount);
        response.setReason(reason);
        response.setRefundedBy(refundedBy);
        response.setRefundedAt(LocalDateTime.now().toString());
        response.setMessage("Refund processed successfully");

        return response;
    }
}
