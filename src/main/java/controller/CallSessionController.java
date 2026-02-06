package controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dto.apiResponse;
import entity.CallSession;
import entity.CallSession.CallType;
import exception.UnauthorizedException;
import service.CallSessionService;

@RestController
@RequestMapping("/api/calls")
@Validated
public class CallSessionController {

    private final CallSessionService callSessionService;

    public CallSessionController(CallSessionService callSessionService) {
        this.callSessionService = callSessionService;
    }

    /**
     * Get the authenticated user ID from security context
     */
    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }
        return auth.getName();
    }

    /**
     * Verify user is authorized to access this call session
     */
    private void verifyCallAccess(CallSession callSession, String userId) {
        if (!callSession.getCallerId().equals(userId) && !callSession.getCalleeId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to access this call session");
        }
    }

    // Start a new call (audio or video)
    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('CLIENT', 'LAWYER')")
    public ResponseEntity<apiResponse<CallSession>> startCall(
            @RequestParam String calleeId,
            @RequestParam(required = false) String appointmentId,
            @RequestParam CallType callType) {

        String callerId = getAuthenticatedUserId();

        CallSession callSession = callSessionService.startCall(callerId, calleeId, appointmentId, callType);
        return ResponseEntity.ok(new apiResponse<>("Call started successfully", true, callSession));
    }

    // Connect a call (when the callee answers)
    @PutMapping("/{callSessionId}/connect")
    @PreAuthorize("hasAnyRole('CLIENT', 'LAWYER')")
    public ResponseEntity<apiResponse<CallSession>> connectCall(@PathVariable Long callSessionId) {
        String userId = getAuthenticatedUserId();

        CallSession callSession = callSessionService.getCallSession(callSessionId)
                .orElseThrow(() -> new UnauthorizedException("Call session not found"));

        verifyCallAccess(callSession, userId);
        CallSession connectedSession = callSessionService.connectCall(callSessionId);

        return ResponseEntity.ok(new apiResponse<>("Call connected successfully", true, connectedSession));
    }

    // End a call
    @PutMapping("/{callSessionId}/end")
    @PreAuthorize("hasAnyRole('CLIENT', 'LAWYER')")
    public ResponseEntity<apiResponse<CallSession>> endCall(@PathVariable Long callSessionId) {
        String userId = getAuthenticatedUserId();

        CallSession callSession = callSessionService.getCallSession(callSessionId)
                .orElseThrow(() -> new UnauthorizedException("Call session not found"));

        verifyCallAccess(callSession, userId);
        CallSession endedSession = callSessionService.endCall(callSessionId);

        return ResponseEntity.ok(new apiResponse<>("Call ended successfully", true, endedSession));
    }

    // Cancel a call (before it's answered)
    @PutMapping("/{callSessionId}/cancel")
    @PreAuthorize("hasAnyRole('CLIENT', 'LAWYER')")
    public ResponseEntity<apiResponse<CallSession>> cancelCall(@PathVariable Long callSessionId) {
        String userId = getAuthenticatedUserId();

        CallSession callSession = callSessionService.getCallSession(callSessionId)
                .orElseThrow(() -> new UnauthorizedException("Call session not found"));

        verifyCallAccess(callSession, userId);
        CallSession cancelledSession = callSessionService.cancelCall(callSessionId);

        return ResponseEntity.ok(new apiResponse<>("Call cancelled successfully", true, cancelledSession));
    }

    // Mark a call as missed (when caller hangs up without answer)
    @PutMapping("/{callSessionId}/missed")
    @PreAuthorize("hasAnyRole('CLIENT', 'LAWYER')")
    public ResponseEntity<apiResponse<CallSession>> markAsMissed(@PathVariable Long callSessionId) {
        String userId = getAuthenticatedUserId();

        CallSession callSession = callSessionService.getCallSession(callSessionId)
                .orElseThrow(() -> new UnauthorizedException("Call session not found"));

        verifyCallAccess(callSession, userId);
        CallSession missedSession = callSessionService.markAsMissed(callSessionId);

        return ResponseEntity.ok(new apiResponse<>("Call marked as missed", true, missedSession));
    }

    // Get call session details
    @GetMapping("/{callSessionId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'LAWYER', 'ADMIN')")
    public ResponseEntity<apiResponse<CallSession>> getCallSession(@PathVariable Long callSessionId) {
        String userId = getAuthenticatedUserId();

        CallSession callSession = callSessionService.getCallSession(callSessionId)
                .orElseThrow(() -> new UnauthorizedException("Call session not found"));

        // Admin can access any session, users can only access their own
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            verifyCallAccess(callSession, userId);
        }

        return ResponseEntity.ok(new apiResponse<>("Call session found", true, callSession));
    }

    // Get call history for authenticated user
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('CLIENT', 'LAWYER')")
    public ResponseEntity<apiResponse<List<CallSession>>> getMyCallHistory(
            @RequestParam(required = false) CallType callType) {
        String userId = getAuthenticatedUserId();

        List<CallSession> callHistory;
        if (callType != null) {
            callHistory = callSessionService.getCallHistoryByUserAndType(userId, callType);
        } else {
            callHistory = callSessionService.getCallHistoryByUser(userId);
        }

        return ResponseEntity.ok(new apiResponse<>("Call history retrieved", true, callHistory));
    }

    // Get call history for a specific user (Admin only)
    @GetMapping("/history/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<apiResponse<List<CallSession>>> getUserCallHistory(
            @PathVariable String userId,
            @RequestParam(required = false) CallType callType) {

        List<CallSession> callHistory;
        if (callType != null) {
            callHistory = callSessionService.getCallHistoryByUserAndType(userId, callType);
        } else {
            callHistory = callSessionService.getCallHistoryByUser(userId);
        }

        return ResponseEntity.ok(new apiResponse<>("User call history retrieved", true, callHistory));
    }

    // Get total call duration for authenticated user
    @GetMapping("/duration")
    @PreAuthorize("hasAnyRole('CLIENT', 'LAWYER')")
    public ResponseEntity<apiResponse<Long>> getMyTotalCallDuration() {
        String userId = getAuthenticatedUserId();
        Long totalDuration = callSessionService.getTotalCallDurationByUser(userId);
        return ResponseEntity.ok(new apiResponse<>("Total call duration retrieved", true, totalDuration));
    }

    // Get total call duration by type (Admin only)
    @GetMapping("/duration/type")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<apiResponse<Long>> getTotalCallDurationByType(@RequestParam CallType callType) {
        Long totalDuration = callSessionService.getTotalCallDurationByType(callType);
        return ResponseEntity.ok(new apiResponse<>("Total call duration by type retrieved", true, totalDuration));
    }

    // Request body for starting a call
    public static class StartCallRequest {
        private String calleeId;
        private String appointmentId;
        private CallType callType;

        public String getCalleeId() {
            return calleeId;
        }

        public void setCalleeId(String calleeId) {
            this.calleeId = calleeId;
        }

        public String getAppointmentId() {
            return appointmentId;
        }

        public void setAppointmentId(String appointmentId) {
            this.appointmentId = appointmentId;
        }

        public CallType getCallType() {
            return callType;
        }

        public void setCallType(CallType callType) {
            this.callType = callType;
        }
    }
}
