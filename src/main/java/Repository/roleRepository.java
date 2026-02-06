package Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import entity.roles;

public interface roleRepository extends JpaRepository<roles,Long> {
 Optional<roles> findByName(String name);
}
