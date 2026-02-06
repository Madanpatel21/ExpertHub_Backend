package Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import entity.Appointment;
import entity.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByLawyerIdAndDateTimeAndStatus(
        Long lawyerId,
        LocalDateTime dateTime,
        AppointmentStatus status
    );

    List<Appointment> findByClientId(Long clientId);

    List<Appointment> findByLawyerId(Long lawyerId);
}