package Repository;

import entity.RazorpayWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RazorpayWebhookEventRepository extends JpaRepository<RazorpayWebhookEvent, Long> {
}
