package Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import entity.Notification;

import java.util.List;

@Repository
public interface NotificationRepo extends CrudRepository<Notification, Long> {

    List<Notification> findByUserId(String userId);

    @Query(value = """
            SELECT * FROM notification
            WHERE user_id = :userId
            AND notification_id = :notificationId
            AND content = :content
            """, nativeQuery = true)
    Notification findDuplicateNotification(
            @Param("userId") String userId,
            @Param("notificationId") String notificationId,
            @Param("content") String content);
}