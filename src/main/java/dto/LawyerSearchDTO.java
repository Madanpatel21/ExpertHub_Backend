package dto;

import lombok.Data;
import java.util.List;

@Data
public class LawyerSearchDTO {

    // SEARCH
    private String name;
    private String location; // city / state / country
    private String practiceAreas;

    // SORT OPTIONS
    private Boolean sortByRatingDesc;
    private Boolean sortByExperienceDesc;
    private Boolean sortByLocation;
    private Boolean sortByPracticeArea;
    private Boolean sortByFeesAsc;

    // GEOLOCATION
    private Double userLat;
    private Double userLong;
    private Double maxDistanceKm;

    // PAGINATION
    private int page = 0;
    private int size = 10;

    // ADVANCED FILTERS
    private Double minRating;
    private Boolean onlyAvailable;
    private Long lawyerId; // Search by specific ID
}