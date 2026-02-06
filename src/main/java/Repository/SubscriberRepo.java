package Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import entity.Subscriber;
import entity.Subscriber.SubscriptionStatus;

@Repository
public interface SubscriberRepo extends JpaRepository<Subscriber, Long> {

	@Query("SELECT COUNT(s) > 0 FROM Subscriber s " +
			"WHERE s.userId = :userId " +
			"AND s.planType = 'main' " +
			"AND s.endDate >= :currentDate " +
			"AND (s.endDate > :currentDate OR (s.endDate = :currentDate AND s.endTime >= :currentTime))")
	boolean existsByUserIdAndEndDateAfterAndEndTimeAfterMain(
			@Param("userId") Long userId,
			@Param("currentDate") LocalDate currentDate,
			@Param("currentTime") LocalTime currentTime);

	@Query("SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END FROM Subscriber s "
			+ "WHERE s.userId = :userId " + "AND s.endDate >= :currentDate "
			+ "AND (s.endDate > :currentDate OR (s.endDate = :currentDate AND s.endTime >= :currentTime))")
	boolean existsByUserIdAndEndDateAfterAndEndTimeAfterOther(Long userId, LocalDate currentDate,
			LocalTime currentTime);

	@Query(value = "SELECT * FROM subscriber WHERE user_id = :userId AND plan_type = 'main'", nativeQuery = true)
	Subscriber findByUserIds(Long userId);

	Optional<Subscriber> findById(Long id);

	@Query("SELECT s FROM Subscriber s WHERE s.userId = :userId AND s.planType = :planType")
	Optional<Subscriber> findByUserIdAndPlanType(@Param("userId") Long userId, @Param("planType") String planType);

	@Query(value = "SELECT * FROM subscriber WHERE user_id = :userId AND plan_type = 'main'", nativeQuery = true)
	Subscriber findActivePlanType(@Param("userId") Long userId);

	@Query("SELECT s.planType, s.description FROM Subscriber s " +
			"WHERE s.userId = :userId " +
			"AND (s.endDate > :currentDate OR (s.endDate = :currentDate AND s.endTime >= :currentTime))")
	List<Object[]> findPlanDescriptionsByUserId(@Param("userId") Long userId,
			@Param("currentDate") LocalDate currentDate,
			@Param("currentTime") LocalTime currentTime);

	@Query(value = "SELECT * FROM subscriber " +
			"WHERE user_id = :userId " +
			"AND (end_date > :currentDate OR (end_date = :currentDate AND end_time >= :currentTime))", nativeQuery = true)
	List<Subscriber> findByUserIds(@Param("userId") Long userId,
			@Param("currentDate") LocalDate currentDate,
			@Param("currentTime") LocalTime currentTime);

	@Query("SELECT s FROM Subscriber s WHERE s.userId = :userId AND s.planType IN ('Astro', 'Relationship')")
	List<Subscriber> findActiveAstroOrRelationshipPlan(@Param("userId") Long userId);

	// ========== Cancellation Methods ==========

	@Query("SELECT s FROM Subscriber s WHERE s.userId = :userId AND s.status = :status")
	List<Subscriber> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") SubscriptionStatus status);

	@Query("SELECT s FROM Subscriber s WHERE s.userId = :userId AND s.status = 'ACTIVE'")
	List<Subscriber> findActiveSubscriptionsByUserId(@Param("userId") Long userId);

	@Query("SELECT s FROM Subscriber s WHERE s.subscriptionId = :razorpaySubscriptionId")
	Optional<Subscriber> findByRazorpaySubscriptionId(@Param("razorpaySubscriptionId") String razorpaySubscriptionId);

	@Modifying
	@Query("UPDATE Subscriber s SET s.status = :status, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :id")
	int updateStatus(@Param("id") Long id, @Param("status") SubscriptionStatus status);

	@Modifying
	@Query("UPDATE Subscriber s SET s.status = :status, s.cancelledAt = :cancelledAt, " +
			"s.effectiveCancellationDate = :effectiveDate, s.cancellationReason = :reason, " +
			"s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :id")
	int updateCancellationDetails(@Param("id") Long id,
			@Param("status") SubscriptionStatus status,
			@Param("cancelledAt") java.time.LocalDateTime cancelledAt,
			@Param("effectiveDate") java.time.LocalDateTime effectiveDate,
			@Param("reason") String reason);

	@Modifying
	@Query("UPDATE Subscriber s SET s.refundProcessed = true, s.refundProcessedAt = :refundDate, " +
			"s.refundId = :refundId, s.refundAmount = :amount, s.refundReason = :refundReason, " +
			"s.status = 'REFUNDED', s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :id")
	int updateRefundDetails(@Param("id") Long id,
			@Param("refundDate") java.time.LocalDateTime refundDate,
			@Param("refundId") String refundId,
			@Param("amount") Double amount,
			@Param("refundReason") String refundReason);

	@Query("SELECT s FROM Subscriber s WHERE s.userId = :userId AND s.status <> 'EXPIRED'")
	List<Subscriber> findNonExpiredByUserId(@Param("userId") Long userId);

	@Query("SELECT COUNT(s) FROM Subscriber s WHERE s.userId = :userId AND s.status = 'ACTIVE'")
	long countActiveSubscriptions(@Param("userId") Long userId);
}
