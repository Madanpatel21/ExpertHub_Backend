package controller;

import dto.AppointmentRequest;
import dto.AppointmentResponse;
import entity.Appointment;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import service.AppointmentService;

@RestController
@RequestMapping("/api/client/appointments")
public class ClientAppointmentController {
    private final AppointmentService appointmentService;

    public ClientAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<?> bookAppointment(
            @RequestBody AppointmentRequest request,
            HttpServletRequest httpRequest) {

        Long clientId = (Long) httpRequest.getAttribute("userId");

        Appointment appointment = appointmentService.bookAppointment(
                clientId,
                request.getLawyerId(),
                request.getDateTime());

        return ResponseEntity.ok(
                new AppointmentResponse(
                        appointment.getId(),
                        appointment.getStatus().name()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long clientId = (Long) request.getAttribute("userId");

        Appointment appointment = appointmentService.cancelAppointment(id, clientId);

        return ResponseEntity.ok(
                new AppointmentResponse(
                        appointment.getId(),
                        appointment.getStatus().name()));
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<?> rescheduleAppointment(
            @PathVariable Long id,
            @RequestBody AppointmentRequest request,
            HttpServletRequest httpRequest) {
        Long clientId = (Long) httpRequest.getAttribute("userId");
        Appointment appointment = appointmentService.rescheduleAppointment(id, clientId, request.getDateTime());
        return ResponseEntity.ok(
                new AppointmentResponse(
                        appointment.getId(),
                        appointment.getStatus().name()));
    }

    @GetMapping
    public ResponseEntity<?> getClientAppointments(HttpServletRequest request) {
        Long clientId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(
                appointmentService.getClientAppointments(clientId));
    }
}