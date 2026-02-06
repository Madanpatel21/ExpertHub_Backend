package entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


@Entity
@Data
@Table(name = "subscriptionDetails")
public class SubscriptionDetails {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	private int amount;
	private String contactView;
	private String type;
	private int duration;
	private Integer discountAmount = 0;
	private String description;
	private String remainingDays;
	private String activePlaneName;

}
