package Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import entity.SupportTicket;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
	List<SupportTicket> findByUserId(Long userId);
}
