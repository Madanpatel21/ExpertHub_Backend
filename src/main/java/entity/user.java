package entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class user {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // User ID

    @Column(nullable = false, unique = true, name = "email")
    private String email; // User email

    @Column(nullable = false, name = "password")
    private String password; // User Password

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = roles.class)
    @JoinColumn(name = "role_id", nullable = false)
    private roles role; // user role

    @Column(nullable = false)
    private String phoneNumber;
    @Column(nullable = false)
    private String name;
    private String location;
    private Boolean publicProfile;

    private String profileImagePath;
    @Column(length = 2000)
    private String bio;

    // Preferences
    private Boolean pushNotificationsEnabled = true;
    private Boolean emailNotificationsEnabled = true;

    // OTP for login verification
    private String otp;
}
