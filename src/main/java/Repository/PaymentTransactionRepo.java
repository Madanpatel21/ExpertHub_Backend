package Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import entity.PaymentTransaction;
import entity.PaymentTransaction.PaymentStatus;

@Repository
public interface PaymentTransactionRepo extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByRazorpayOrderId(String orderId);

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByAppointmentIdAndStatus(Long appointmentId, PaymentStatus status);

    Optional<PaymentTransaction> findByAppointmentIdAndStatus(Long appointmentId, PaymentStatus status);
}
