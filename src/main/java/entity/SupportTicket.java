package entity;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SupportTicket {

    public enum SupportCategory {
        GENERAL,
        TECHNICAL,
        PAYMENT,
        OTHERS
    }

    public enum Status {
        OPEN,
        CLOSED,
        PENDING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    @ManyToOne
    @JoinColumn(name = "role_id")
    private roles role;
    private String subject;
    private String message;

    @Enumerated(EnumType.STRING)
    private SupportCategory category;

    @Enumerated(EnumType.STRING)
    private Status status;
    
    private LocalDateTime createdAt;

}