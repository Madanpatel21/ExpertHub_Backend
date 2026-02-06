package Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import entity.passwordResetToken;

@Repository
public interface passwordResetTokenRepository extends JpaRepository<passwordResetToken, Long> {
    Optional<passwordResetToken> findByToken(String token);
    Optional<passwordResetToken> findByUserIdAndIsUsedFalse(Long userId);
    void deleteByExpiryTimeBefore(LocalDateTime dateTime);
}
