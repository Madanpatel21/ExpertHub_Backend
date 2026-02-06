package entity;


import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "report")
public class Report {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String userId;
	private String reportDetails;
	
	@Lob 
	@Column( columnDefinition="LONGBLOB")
	private byte[] reportImage;
	
    private LocalDate date;;
	
    
    // This method sets the date before saving the entity
    @PrePersist
    protected void onCreate() {
        this.date = LocalDate.now();
    }

	
	
}
