package controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Repository.AppointmentRepository;
import dto.apiResponse;
import entity.Appointment;
import entity.AppointmentStatus;
import exception.UnauthorizedException;
import exception.badRequestException;

@RestController
@RequestMapping("/api/appointments")
@Validated
public class AppointmentPaymentController {

    private final AppointmentRepository appointmentRepository;

    public AppointmentPaymentController(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Get the authenticated user ID from security context
     */
    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }
        return auth.getName();
    }

    /**
     * Mark an appointment as paid after successful payment
     * Called after Razorpay payment verification
     * Only the client who booked the appointment can mark it as paid
     */
    @PutMapping("/{appointmentId}/mark-paid")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<apiResponse<Appointment>> markAsPaid(
            @PathVariable Long appointmentId,
            @RequestBody PaymentConfirmationRequest request) {

        String userId = getAuthenticatedUserId();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new badRequestException("Appointment not found"));

        // Verify client owns this appointment
        if (!appointment.getClientId().toString().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to mark this appointment as paid");
        }

        // Check if already paid
        if (Boolean.TRUE.equals(appointment.getIsPaid())) {
            return ResponseEntity.ok(new apiResponse<>("Appointment already paid", true, appointment));
        }

        // Update payment details
        appointment.setIsPaid(true);
        appointment.setPaymentId(request.getPaymentId());
        appointment.setPaymentDate(LocalDateTime.now());
        appointment.setStatus(AppointmentStatus.PAID);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        return ResponseEntity.ok(new apiResponse<>("Appointment marked as paid successfully", true, savedAppointment));
    }

    /**
     * Get payment status for an appointment
     * Both client and lawyer can check payment status
     */
    @GetMapping("/{appointmentId}/payment-status")
    @PreAuthorize("hasAnyRole('CLIENT', 'LAWYER', 'ADMIN')")
    public ResponseEntity<apiResponse<PaymentStatusResponse>> getPaymentStatus(@PathVariable Long appointmentId) {
        String userId = getAuthenticatedUserId();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new badRequestException("Appointment not found"));

        // Verify user is either client or lawyer for this appointment
        boolean isClient = appointment.getClientId().toString().equals(userId);
        boolean isLawyer = appointment.getLawyerId().toString().equals(userId);
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isClient && !isLawyer && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to view this appointment");
        }

        PaymentStatusResponse statusResponse = new PaymentStatusResponse(
                Boolean.TRUE.equals(appointment.getIsPaid()),
                appointment.getPaymentId(),
                appointment.getPaymentDate());

        return ResponseEntity.ok(new apiResponse<>("Payment status retrieved", true, statusResponse));
    }

    /**
     * Verify if client can access chat/call for an appointment
     */
    @PostMapping("/{appointmentId}/verify-access")
    @PreAuthorize("hasAnyRole('CLIENT', 'LAWYER')")
    public ResponseEntity<apiResponse<AccessStatusResponse>> verifyAccess(@PathVariable Long appointmentId) {

        String userId = getAuthenticatedUserId();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new badRequestException("Appointment not found"));

        // Verify user is either client or lawyer for this appointment
        boolean isClient = appointment.getClientId().toString().equals(userId);
        boolean isLawyer = appointment.getLawyerId().toString().equals(userId);

        if (!isClient && !isLawyer) {
            return ResponseEntity.ok(
                    new apiResponse<>("Access denied", false, new AccessStatusResponse(false, "Not authorized")));
        }

        // Check payment status
        boolean isPaid = Boolean.TRUE.equals(appointment.getIsPaid());

        if (!isPaid) {
            return ResponseEntity.ok(
                    new apiResponse<>("Payment required", false,
                            new AccessStatusResponse(false,
                                    "Please complete payment to access chat and call features")));
        }

        // Check appointment status
        if (appointment.getStatus() == AppointmentStatus.CANCELLED ||
                appointment.getStatus() == AppointmentStatus.REJECTED) {
            return ResponseEntity.ok(
                    new apiResponse<>("Appointment inactive", false,
                            new AccessStatusResponse(false, "Appointment is no longer active")));
        }

        return ResponseEntity.ok(
                new apiResponse<>("Access granted", true, new AccessStatusResponse(true, "Payment verified")));
    }

    // Request body for payment confirmation
    public static class PaymentConfirmationRequest {
        private String paymentId;

        public String getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(String paymentId) {
            this.paymentId = paymentId;
        }
    }

    // Response for payment status
    public static class PaymentStatusResponse {
        private boolean isPaid;
        private String paymentId;
        private LocalDateTime paymentDate;

        public PaymentStatusResponse(boolean isPaid, String paymentId, LocalDateTime paymentDate) {
            this.isPaid = isPaid;
            this.paymentId = paymentId;
            this.paymentDate = paymentDate;
        }

        public boolean isPaid() {
            return isPaid;
        }

        public String getPaymentId() {
            return paymentId;
        }

        public LocalDateTime getPaymentDate() {
            return paymentDate;
        }
    }

    // Response for access verification
    public static class AccessStatusResponse {
        private boolean hasAccess;
        private String message;

        public AccessStatusResponse(boolean hasAccess, String message) {
            this.hasAccess = hasAccess;
            this.message = message;
        }

        public boolean isHasAccess() {
            return hasAccess;
        }

        public String getMessage() {
            return message;
        }
    }
}
