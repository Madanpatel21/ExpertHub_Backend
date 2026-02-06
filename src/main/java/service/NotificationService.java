package service;

import entity.Notification;

public interface NotificationService {

    Notification createNotification(
            String userId,
            String notificationId,
            String content);

    @org.springframework.scheduling.annotation.Async
    void sendPushNotification(String userId, String title, String body);

    void markAsRead(Long id);

    void markAllAsRead(String userId);
}
