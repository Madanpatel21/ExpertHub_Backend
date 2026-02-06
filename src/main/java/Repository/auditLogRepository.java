package Repository;

import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;

import entity.auditLog;


public interface auditLogRepository extends JpaRepository<auditLog,Long> {
	List<auditLog> findByActor(String Actor);
	List<auditLog> findByAction(String Action);
	List<auditLog> findAllByOrderByCreatedAtDesc();
}
