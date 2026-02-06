package Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import entity.Registration;

@Repository
public interface RepositorytwoRepo extends JpaRepository<Registration, Long> {
	Registration findByPhoneNumber(String phoneNumber);
}
