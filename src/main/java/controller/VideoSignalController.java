package controller;

import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import entity.CallSession;
import entity.CallSession.CallType;
import service.CallSessionService;
import service.PaymentVerificationService;

@Controller
public class VideoSignalController {

    private final CallSessionService callSessionService;
    private final PaymentVerificationService paymentVerificationService;

    public VideoSignalController(CallSessionService callSessionService,
            PaymentVerificationService paymentVerificationService) {
        this.callSessionService = callSessionService;
        this.paymentVerificationService = paymentVerificationService;
    }

    @MessageMapping("/offer")
    @SendTo("/topic/offer")
    public Map<String, Object> handleOffer(Map<String, Object> data) {
        // Verify payment before allowing call
        String appointmentIdStr = (String) data.get("appointmentId");
        String callerId = (String) data.get("callerId");

        if (appointmentIdStr != null && callerId != null) {
            try {
                Long appointmentId = Long.parseLong(appointmentIdStr);
                Long userId = Long.parseLong(callerId);

                boolean hasAccess = paymentVerificationService.canAccessCall(appointmentId, userId);
                if (!hasAccess) {
                    throw new securityException("Payment required to initiate calls");
                }
            } catch (NumberFormatException e) {
                throw new securityException("Invalid appointment or user ID");
            }
        }

        // Extract call session ID from the offer message
        String callSessionIdStr = (String) data.get("callSessionId");
        String calleeId = (String) data.get("calleeId");
        String callTypeStr = (String) data.get("callType");

        // Start call tracking if not already started
        if (callSessionIdStr == null && callerId != null && calleeId != null) {
            CallType callType = "VIDEO".equalsIgnoreCase(callTypeStr) ? CallType.VIDEO : CallType.AUDIO;
            CallSession callSession = callSessionService.startCall(callerId, calleeId, appointmentIdStr, callType);
            data.put("callSessionId", callSession.getId().toString());
        } else if (callSessionIdStr != null) {
            // Connect the call when offer is sent
            try {
                Long callSessionId = Long.parseLong(callSessionIdStr);
                callSessionService.connectCall(callSessionId);
            } catch (NumberFormatException e) {
                // Ignore invalid call session ID
            }
        }

        return data;
    }

    @MessageMapping("/answer")
    @SendTo("/topic/answer")
    public Map<String, Object> handleAnswer(Map<String, Object> data) {
        // Verify payment before allowing call
        String appointmentIdStr = (String) data.get("appointmentId");
        String calleeId = (String) data.get("calleeId");

        if (appointmentIdStr != null && calleeId != null) {
            try {
                Long appointmentId = Long.parseLong(appointmentIdStr);
                Long userId = Long.parseLong(calleeId);

                boolean hasAccess = paymentVerificationService.canAccessCall(appointmentId, userId);
                if (!hasAccess) {
                    throw new securityException("Payment required to join calls");
                }
            } catch (NumberFormatException e) {
                throw new securityException("Invalid appointment or user ID");
            }
        }

        // Mark call as connected when answer is received
        String callSessionIdStr = (String) data.get("callSessionId");
        if (callSessionIdStr != null) {
            try {
                Long callSessionId = Long.parseLong(callSessionIdStr);
                callSessionService.connectCall(callSessionId);
            } catch (NumberFormatException e) {
                // Ignore invalid call session ID
            }
        }
        return data;
    }

    @MessageMapping("/candidate")
    @SendTo("/topic/candidate")
    public Map<String, Object> handleCandidate(Map<String, Object> data) {
        return data;
    }

    @MessageMapping("/hangup")
    @SendTo("/topic/hangup")
    public Map<String, Object> handleHangup(Map<String, Object> data) {
        // End call tracking when hangup signal is received
        String callSessionIdStr = (String) data.get("callSessionId");
        if (callSessionIdStr != null) {
            try {
                Long callSessionId = Long.parseLong(callSessionIdStr);
                callSessionService.endCall(callSessionId);
            } catch (NumberFormatException e) {
                // Ignore invalid call session ID
            }
        }
        return data;
    }

    @MessageMapping("/reject")
    @SendTo("/topic/reject")
    public Map<String, Object> handleReject(Map<String, Object> data) {
        // Mark call as missed when rejected
        String callSessionIdStr = (String) data.get("callSessionId");
        if (callSessionIdStr != null) {
            try {
                Long callSessionId = Long.parseLong(callSessionIdStr);
                callSessionService.markAsMissed(callSessionId);
            } catch (NumberFormatException e) {
                // Ignore invalid call session ID
            }
        }
        return data;
    }

    // Custom security exception for payment verification
    public static class securityException extends RuntimeException {
        public securityException(String message) {
            super(message);
        }
    }
}
