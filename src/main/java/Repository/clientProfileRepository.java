package Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import entity.clientProfile;
import entity.user;

public interface clientProfileRepository extends JpaRepository<clientProfile, Long> {
	Optional<clientProfile> findByUser(user user);
}
