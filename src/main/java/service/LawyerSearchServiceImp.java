package service;

import dto.LawyerSearchDTO;
import entity.lawyerProfile;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import Repository.lawyerProfileRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LawyerSearchServiceImp implements LawyerSearchServiceInter {

        private final lawyerProfileRepository lawyerRepo;

        @Override
        @org.springframework.cache.annotation.Cacheable(value = "lawyer_search", key = "#dto")
        public List<lawyerProfile> searchAndSortLawyers(LawyerSearchDTO dto) {
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest
                                .of(dto.getPage(), dto.getSize());

                org.springframework.data.jpa.domain.Specification<lawyerProfile> spec = org.springframework.data.jpa.domain.Specification
                                .where(null);

                if (dto.getLawyerId() != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), dto.getLawyerId()));
                }
                if (dto.getName() != null && !dto.getName().isEmpty()) {
                        spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("fullName")),
                                        "%" + dto.getName().toLowerCase() + "%"));
                }
                if (dto.getLocation() != null && !dto.getLocation().isEmpty()) {
                        spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("city")),
                                        dto.getLocation().toLowerCase()));
                }
                if (dto.getPracticeAreas() != null && !dto.getPracticeAreas().isEmpty()) {
                        spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("practiceArea")),
                                        dto.getPracticeAreas().toLowerCase()));
                }
                if (dto.getMinRating() != null) {
                        spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("averageRating"),
                                        dto.getMinRating()));
                }
                if (Boolean.TRUE.equals(dto.getOnlyAvailable())) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("onlineStatus"), true));
                }

                // Fetch paginated results from DB
                org.springframework.data.domain.Page<lawyerProfile> page = lawyerRepo.findAll(spec, pageable);
                List<lawyerProfile> results = new java.util.ArrayList<>(page.getContent());

                // 🔃 CUSTOM SORTING & DISTANCE CALC
                if (dto.getUserLat() != null && dto.getUserLong() != null) {
                        results.forEach(l -> {
                                if (l.getLatitude() != null && l.getLongitude() != null) {
                                        double dist = calculateDistance(dto.getUserLat(), dto.getUserLong(),
                                                        l.getLatitude(), l.getLongitude());
                                        // We could add a transient field for distance in the entity or return a wrapper
                                        // DTO
                                }
                        });

                        if (dto.getMaxDistanceKm() != null) {
                                results = results.stream()
                                                .filter(l -> l.getLatitude() != null && l.getLongitude() != null &&
                                                                calculateDistance(dto.getUserLat(), dto.getUserLong(),
                                                                                l.getLatitude(),
                                                                                l.getLongitude()) <= dto
                                                                                                .getMaxDistanceKm())
                                                .collect(Collectors.toList());
                        }
                }

                // Apply additional sorting if needed (Rating, Experience, etc.)
                if (Boolean.TRUE.equals(dto.getSortByRatingDesc())) {
                        results.sort(Comparator.comparing(lawyerProfile::getAverageRating,
                                        Comparator.nullsLast(Comparator.reverseOrder())));
                } else if (Boolean.TRUE.equals(dto.getSortByExperienceDesc())) {
                        results.sort(Comparator.comparing(lawyerProfile::getYearsOfExperience).reversed());
                } else if (Boolean.TRUE.equals(dto.getSortByFeesAsc())) {
                        results.sort(Comparator.comparing(lawyerProfile::getConsultationFees));
                }

                return results;
        }

        private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
                double theta = lon1 - lon2;
                double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
                                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                                                * Math.cos(Math.toRadians(theta));
                dist = Math.acos(dist);
                dist = Math.toDegrees(dist);
                dist = dist * 60 * 1.1515 * 1.609344;
                return dist;
        }
}