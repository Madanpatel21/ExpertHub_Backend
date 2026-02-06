package controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import entity.Notification;
import lombok.RequiredArgsConstructor;
import service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @org.springframework.web.bind.annotation.PutMapping("/{id}/read")
    public dto.apiResponse<Void> markAsRead(@org.springframework.web.bind.annotation.PathVariable Long id) {
        notificationService.markAsRead(id);
        return new dto.apiResponse<>("Notification marked as read", true, null);
    }

    @org.springframework.web.bind.annotation.PutMapping("/read-all")
    public dto.apiResponse<Void> markAllAsRead(@org.springframework.web.bind.annotation.RequestParam String userId) {
        notificationService.markAllAsRead(userId);
        return new dto.apiResponse<>("All notifications marked as read", true, null);
    }
}
