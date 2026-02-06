package entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "razorpay_webhook_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private String signature;

    private LocalDateTime receivedAt;

    private boolean processed;

    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
        processed = false;
    }
}
