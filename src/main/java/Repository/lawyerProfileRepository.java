package Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import entity.lawyerProfile;
import entity.user;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface lawyerProfileRepository
        extends JpaRepository<lawyerProfile, Long>, JpaSpecificationExecutor<lawyerProfile> {
    boolean existsByUser(user user);

    List<lawyerProfile> findByVerifiedFalse();

    @Query("""
            SELECT l FROM lawyerProfile l
            WHERE l.verified = true
            AND(:city IS NULL OR l.city = :city)
            AND(:practiceArea IS NULL OR l.practiceArea = :practiceArea)
            AND(:minExperience IS NULL OR l.yearsOfExperience >= :minExperience)
            """)
    List<lawyerProfile> searchLawyers(
            @Param("city") String city,
            @Param("practiceArea") String practiceArea,
            @Param("minExperience") Integer minExperience);

    Optional<lawyerProfile> findByUser(user user);
}
