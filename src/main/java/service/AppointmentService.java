package service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import Repository.AppointmentRepository;
import entity.Appointment;
import entity.AppointmentStatus;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public AppointmentService(AppointmentRepository appointmentRepository, SimpMessagingTemplate messagingTemplate,
            NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    // CLIENT → BOOK
    @Transactional
    public Appointment bookAppointment(Long clientId, Long lawyerId, LocalDateTime dateTime) {

        if (dateTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("PAST_DATE_NOT_ALLOWED");
        }

        boolean conflict = appointmentRepository
                .existsByLawyerIdAndDateTimeAndStatus(
                        lawyerId,
                        dateTime,
                        AppointmentStatus.CONFIRMED);

        if (conflict) {
            throw new RuntimeException("TIME_SLOT_ALREADY_BOOKED");
        }

        Appointment appointment = new Appointment();
        appointment.setClientId(clientId);
        appointment.setLawyerId(lawyerId);
        appointment.setDateTime(dateTime);
        appointment.setStatus(AppointmentStatus.REQUESTED);

        Appointment saved = appointmentRepository.save(appointment);

        // Notify Lawyer
        sendUpdates(lawyerId.toString(), "New Appointment Request", "You have a new booking request for " + dateTime);

        return saved;
    }

    // LAWYER → APPROVE
    @Transactional
    public Appointment approveAppointment(Long appointmentId, Long lawyerId) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("APPOINTMENT_NOT_FOUND"));

        if (!appointment.getLawyerId().equals(lawyerId)) {
            throw new RuntimeException("UNAUTHORIZED");
        }

        if (appointment.getStatus() != AppointmentStatus.REQUESTED) {
            throw new RuntimeException("INVALID_STATE");
        }

        boolean conflict = appointmentRepository
                .existsByLawyerIdAndDateTimeAndStatus(
                        appointment.getLawyerId(),
                        appointment.getDateTime(),
                        AppointmentStatus.CONFIRMED);

        if (conflict) {
            throw new RuntimeException("TIME_SLOT_ALREADY_BOOKED");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment saved = appointmentRepository.save(appointment);

        // Notify Client
        sendUpdates(appointment.getClientId().toString(), "Appointment Confirmed",
                "Your appointment on " + appointment.getDateTime() + " has been confirmed.");

        return saved;
    }

    // LAWYER → REJECT
    @Transactional
    public Appointment rejectAppointment(Long appointmentId, Long lawyerId) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("APPOINTMENT_NOT_FOUND"));

        if (!appointment.getLawyerId().equals(lawyerId)) {
            throw new RuntimeException("UNAUTHORIZED");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);

        // Notify Client
        sendUpdates(appointment.getClientId().toString(), "Appointment Rejected",
                "Your appointment request has been declined.");

        return saved;
    }

    // CLIENT → CANCEL
    @Transactional
    public Appointment cancelAppointment(Long appointmentId, Long clientId) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("APPOINTMENT_NOT_FOUND"));

        if (!appointment.getClientId().equals(clientId)) {
            throw new RuntimeException("UNAUTHORIZED");
        }

        if (appointment.getDateTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("APPOINTMENT_ALREADY_STARTED");
        }

        if (!(appointment.getStatus() == AppointmentStatus.REQUESTED
                || appointment.getStatus() == AppointmentStatus.CONFIRMED)) {
            throw new RuntimeException("CANNOT_CANCEL");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);

        // Notify Lawyer
        sendUpdates(appointment.getLawyerId().toString(), "Appointment Cancelled",
                "Client has cancelled the appointment for " + appointment.getDateTime());

        return saved;
    }

    // CLIENT → RESCHEDULE
    @Transactional
    public Appointment rescheduleAppointment(Long appointmentId, Long clientId, LocalDateTime newDateTime) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("APPOINTMENT_NOT_FOUND"));

        if (!appointment.getClientId().equals(clientId)) {
            throw new RuntimeException("UNAUTHORIZED");
        }

        if (newDateTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("PAST_DATE_NOT_ALLOWED");
        }

        // Check for conflicts on new date
        boolean conflict = appointmentRepository.existsByLawyerIdAndDateTimeAndStatus(
                appointment.getLawyerId(), newDateTime, AppointmentStatus.CONFIRMED);

        if (conflict) {
            throw new RuntimeException("TIME_SLOT_ALREADY_BOOKED");
        }

        appointment.setDateTime(newDateTime);
        appointment.setStatus(AppointmentStatus.REQUESTED); // Move back to requested for approval
        Appointment saved = appointmentRepository.save(appointment);

        // Notify Lawyer
        sendUpdates(appointment.getLawyerId().toString(), "Reschedule Request",
                "Client requested to reschedule appointment to " + newDateTime);

        return saved;
    }

    private void sendUpdates(String userId, String title, String body) {
        // 1. WebSocket Update
        messagingTemplate.convertAndSend("/topic/appointments/" + userId, body);

        // 2. Push Notification
        notificationService.sendPushNotification(userId, title, body);

        // 3. Persistent Notification
        notificationService.createNotification(userId, "APP_" + System.currentTimeMillis(), body);
    }

    // VIEW
    public List<Appointment> getClientAppointments(Long clientId) {
        return appointmentRepository.findByClientId(clientId);
    }

    public List<Appointment> getLawyerAppointments(Long lawyerId) {
        return appointmentRepository.findByLawyerId(lawyerId);
    }
}
