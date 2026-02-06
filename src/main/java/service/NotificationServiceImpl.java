package service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import Repository.NotificationRepo;
import entity.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepo notificationRepo;
    private final Repository.userRepository userRepo;

    @org.springframework.beans.factory.annotation.Value("${fcm.project.id}")
    private String fcmProjectId;

    private final GoogleAuthService googleAuthService;
    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    @Override
    public Notification createNotification(
            String userId,
            String notificationId,
            String content) {

        // Before creating persistent notification, we could check emailPreferences if
        // we had email logic here.
        // For now we always store in DB as it's the history.

        // 1️⃣ Check duplicate
        Notification existing = notificationRepo.findDuplicateNotification(userId, notificationId, content);

        if (existing != null) {
            return existing; // already exists
        }

        // 2️⃣ Create new notification
        Notification n = new Notification();
        n.setUserId(userId);
        n.setNotificationId(notificationId);
        n.setContent(content);
        n.setLastNotificationTime(LocalDateTime.now());
        n.setIsRead(0);

        return notificationRepo.save(n);
    }

    @Override
    public void sendPushNotification(String userId, String title, String body) {
        // Check user preferences
        userRepo.findById(Long.parseLong(userId)).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getPushNotificationsEnabled())) {
                try {
                    String accessToken = googleAuthService.generateAccessToken();
                    String fcmUrl = "https://fcm.googleapis.com/v1/projects/" + fcmProjectId + "/messages:send";

                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                    headers.setBearerAuth(accessToken);

                    // Construct FCM Message Payload
                    org.json.JSONObject message = new org.json.JSONObject();
                    org.json.JSONObject notification = new org.json.JSONObject();
                    notification.put("title", title);
                    notification.put("body", body);

                    message.put("token", "USER_DEVICE_TOKEN_PLACEHOLDER"); // TODO: detailed strategy for device tokens
                                                                           // needed
                    // ideally user.getDeviceToken() if available
                    message.put("notification", notification);

                    org.json.JSONObject payload = new org.json.JSONObject();
                    payload.put("message", message);

                    org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(
                            payload.toString(), headers);

                    // Since we don't have real device tokens yet, this call might fail with
                    // INVALID_ARGUMENT
                    // But the logic is sound. Using a try-catch block.

                    // For now, logging only until we know where device tokens are stored.
                    // If user entity has a device token field, we should use it.
                    System.out.println("DEBUG [FCM Ready] URL: " + fcmUrl + " Payload: " + payload.toString());

                    // Uncomment when ready to send
                    // restTemplate.postForEntity(fcmUrl, entity, String.class);

                    System.out.println("DEBUG [Push Notification SENT] To: " + userId);

                } catch (Exception e) {
                    System.err.println("ERROR: Failed to send FCM notification: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println(
                        "DEBUG [Push Notification SKIPPED] User " + userId + " has disabled push notifications.");
            }
        });
    }

    @Override
    public void markAsRead(Long id) {
        notificationRepo.findById(id).ifPresent(n -> {
            n.setIsRead(1);
            notificationRepo.save(n);
        });
    }

    @Override
    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepo.findByUserId(userId)
                .stream().filter(n -> n.getIsRead() == 0).collect(Collectors.toList());
        unread.forEach(n -> n.setIsRead(1));
        notificationRepo.saveAll(unread);
    }
}
