package entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "call_sessions")
public class CallSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String callerId;
    private String calleeId;
    private String appointmentId;

    @Enumerated(EnumType.STRING)
    private CallType callType;

    @Enumerated(EnumType.STRING)
    private CallStatus status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationInSeconds;

    public enum CallType {
        AUDIO,
        VIDEO
    }

    public enum CallStatus {
        INITIATED,
        CONNECTED,
        COMPLETED,
        MISSED,
        CANCELLED
    }
}
