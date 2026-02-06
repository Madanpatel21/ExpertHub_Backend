package service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import Repository.CallSessionRepo;
import entity.CallSession;
import entity.CallSession.CallStatus;
import entity.CallSession.CallType;
import exception.badRequestException;

@Service
public class CallSessionService {

    private final CallSessionRepo callSessionRepo;

    public CallSessionService(CallSessionRepo callSessionRepo) {
        this.callSessionRepo = callSessionRepo;
    }

    // Start a new call session
    public CallSession startCall(String callerId, String calleeId, String appointmentId, CallType callType) {
        CallSession callSession = new CallSession();
        callSession.setCallerId(callerId);
        callSession.setCalleeId(calleeId);
        callSession.setAppointmentId(appointmentId);
        callSession.setCallType(callType);
        callSession.setStatus(CallStatus.INITIATED);
        callSession.setStartTime(LocalDateTime.now());

        return callSessionRepo.save(callSession);
    }

    // Update call status to CONNECTED when the call is answered
    public CallSession connectCall(Long callSessionId) {
        CallSession callSession = callSessionRepo.findById(callSessionId)
                .orElseThrow(() -> new badRequestException("Call session not found"));

        if (callSession.getStatus() == CallStatus.INITIATED) {
            callSession.setStatus(CallStatus.CONNECTED);
            return callSessionRepo.save(callSession);
        }

        throw new badRequestException("Call is not in INITIATED status");
    }

    // End a call and calculate duration
    public CallSession endCall(Long callSessionId) {
        CallSession callSession = callSessionRepo.findById(callSessionId)
                .orElseThrow(() -> new badRequestException("Call session not found"));

        if (callSession.getStatus() == CallStatus.COMPLETED || callSession.getStatus() == CallStatus.CANCELLED) {
            throw new badRequestException("Call is already completed or cancelled");
        }

        LocalDateTime endTime = LocalDateTime.now();
        callSession.setEndTime(endTime);
        callSession.setStatus(CallStatus.COMPLETED);

        // Calculate duration in seconds
        if (callSession.getStartTime() != null) {
            Duration duration = Duration.between(callSession.getStartTime(), endTime);
            callSession.setDurationInSeconds(duration.getSeconds());
        }

        return callSessionRepo.save(callSession);
    }

    // Mark call as missed
    public CallSession markAsMissed(Long callSessionId) {
        CallSession callSession = callSessionRepo.findById(callSessionId)
                .orElseThrow(() -> new badRequestException("Call session not found"));

        callSession.setStatus(CallStatus.MISSED);
        callSession.setEndTime(LocalDateTime.now());

        if (callSession.getStartTime() != null) {
            Duration duration = Duration.between(callSession.getStartTime(), callSession.getEndTime());
            callSession.setDurationInSeconds(duration.getSeconds());
        }

        return callSessionRepo.save(callSession);
    }

    // Cancel a call before it's answered
    public CallSession cancelCall(Long callSessionId) {
        CallSession callSession = callSessionRepo.findById(callSessionId)
                .orElseThrow(() -> new badRequestException("Call session not found"));

        if (callSession.getStatus() == CallStatus.INITIATED) {
            callSession.setStatus(CallStatus.CANCELLED);
            callSession.setEndTime(LocalDateTime.now());
            return callSessionRepo.save(callSession);
        }

        throw new badRequestException("Can only cancel calls in INITIATED status");
    }

    // Get call session by ID
    public Optional<CallSession> getCallSession(Long id) {
        return callSessionRepo.findById(id);
    }

    // Get all call sessions for a user
    public List<CallSession> getCallHistoryByUser(String userId) {
        return callSessionRepo.findAllByUserId(userId);
    }

    // Get call sessions by type (AUDIO or VIDEO)
    public List<CallSession> getCallHistoryByUserAndType(String userId, CallType callType) {
        return callSessionRepo.findByUserIdAndCallType(userId, callType);
    }

    // Get total call duration for a user
    public Long getTotalCallDurationByUser(String userId) {
        Long totalDuration = callSessionRepo.getTotalCallDurationByUser(userId);
        return totalDuration != null ? totalDuration : 0L;
    }

    // Get total call duration by type
    public Long getTotalCallDurationByType(CallType callType) {
        Long totalDuration = callSessionRepo.getTotalCallDurationByType(callType);
        return totalDuration != null ? totalDuration : 0L;
    }
}
