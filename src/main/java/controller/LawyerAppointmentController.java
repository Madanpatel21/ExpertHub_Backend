package controller;

import jakarta.servlet.http.HttpServletRequest;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import service.AppointmentService;

@RestController
@RequestMapping("/api/lawyer/appointments")
public class LawyerAppointmentController {

    private final AppointmentService appointmentService;

    public LawyerAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // Lawyer views appointments
    @GetMapping
    public ResponseEntity<?> getAppointments(HttpServletRequest request) {
        Long lawyerId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(
            appointmentService.getLawyerAppointments(lawyerId)
        );
    }

    // Lawyer approves appointment
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long lawyerId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(
            appointmentService.approveAppointment(id, lawyerId)
        );
    }

    // Lawyer rejects appointment
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long lawyerId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(
            appointmentService.rejectAppointment(id, lawyerId)
        );
    }
}
