package Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.yaml.snakeyaml.events.Event.ID;

import entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long>{
	List<Review> findByLawyerId(Long lawyerId);

    boolean existsByClientIdAndAppointmentId(
        Long clientId,
        Long appointmentId
    );

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.lawyerId = :lawyerId")
    Double getAverageRating(Long lawyerId);
}
