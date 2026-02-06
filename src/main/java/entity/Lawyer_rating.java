package entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "Lawyer_Ratings")
@Getter
@Setter
public class Lawyer_rating {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	 private Long id;
	 private int Rating;
	 
	 @Column(length = 1000)
	 private String Review;
	 
	 @ManyToOne
	 @JoinColumn(name = "lawyer_id")
	 private lawyerProfile lawyer;
	 
	 @ManyToOne
	 @JoinColumn(name = "client_id")
	 private user client;
	 
	 
	 @OneToOne
	 @JoinColumn(name = "appointment_id", unique = true)
	 private Appointment appoinment;
	 
	 private LocalDateTime createdAt;
	 
}
