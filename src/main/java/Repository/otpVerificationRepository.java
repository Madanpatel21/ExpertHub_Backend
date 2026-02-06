package Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import entity.otpVerfication;

public interface otpVerificationRepository extends JpaRepository<otpVerfication, Long> {
	Optional<otpVerfication> findByEmailAndVerifiedFalse(String email);

    void deleteByEmail(String email);
}
