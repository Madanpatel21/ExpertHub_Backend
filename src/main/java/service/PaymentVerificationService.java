package service;

import org.springframework.stereotype.Service;

import Repository.AppointmentRepository;
import entity.Appointment;
import entity.AppointmentStatus;
import exception.UnauthorizedException;

@Service
public class PaymentVerificationService {

    private final AppointmentRepository appointmentRepository;

    public PaymentVerificationService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Verify if the client has paid for the appointment
     * 
     * @param appointmentId The appointment ID
     * @param clientId      The client ID to verify ownership
     * @return The appointment if payment is verified
     * @throws UnauthorizedException if payment is not verified or unauthorized
     */
    public Appointment verifyPaymentAndGetAppointment(Long appointmentId, Long clientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new UnauthorizedException("Appointment not found"));

        // Verify the client owns this appointment
        if (!appointment.getClientId().equals(clientId)) {
            throw new UnauthorizedException("You are not authorized to access this appointment");
        }

        // Check if payment is verified
        if (!Boolean.TRUE.equals(appointment.getIsPaid())) {
            throw new UnauthorizedException(
                    "Payment required. Please complete payment to access chat and call features.");
        }

        // Check if appointment is in valid status
        if (appointment.getStatus() == AppointmentStatus.CANCELLED ||
                appointment.getStatus() == AppointmentStatus.REJECTED) {
            throw new UnauthorizedException("Appointment is no longer active");
        }

        return appointment;
    }

    /**
     * Simple check if appointment is paid (without client verification)
     * 
     * @param appointmentId The appointment ID
     * @return true if paid, false otherwise
     */
    public boolean isAppointmentPaid(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .map(Appointment::getIsPaid)
                .orElse(false);
    }

    /**
     * Check if client can access chat for an appointment
     * 
     * @param appointmentId The appointment ID
     * @param userId        The user ID
     * @return true if access is allowed
     */
    public boolean canAccessChat(Long appointmentId, Long userId) {
        try {
            verifyPaymentAndGetAppointment(appointmentId, userId);
            return true;
        } catch (UnauthorizedException e) {
            return false;
        }
    }

    /**
     * Check if client can initiate/join a call for an appointment
     * 
     * @param appointmentId The appointment ID
     * @param userId        The user ID
     * @return true if call access is allowed
     */
    public boolean canAccessCall(Long appointmentId, Long userId) {
        try {
            verifyPaymentAndGetAppointment(appointmentId, userId);
            return true;
        } catch (UnauthorizedException e) {
            return false;
        }
    }
}
