package controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import entity.ChatMessage;
import service.PaymentVerificationService;

@Controller
public class ChatController {

    private final PaymentVerificationService paymentVerificationService;

    public ChatController(PaymentVerificationService paymentVerificationService) {
        this.paymentVerificationService = paymentVerificationService;
    }

    @MessageMapping("/chat") // Client → Server
    @SendTo("/topic/messages") // Server → ALL clients
    public ChatMessage send(ChatMessage message) {
        // Verify payment before allowing chat
        if (message.getAppointmentId() != null && message.getSenderId() != null) {
            boolean hasAccess = paymentVerificationService.canAccessChat(
                    message.getAppointmentId(),
                    Long.parseLong(message.getSenderId()));
            if (!hasAccess) {
                throw new securityException("Payment required to access chat feature");
            }
        }
        return message;
    }

    // Custom security exception for payment verification
    public static class securityException extends RuntimeException {
        public securityException(String message) {
            super(message);
        }
    }
}
