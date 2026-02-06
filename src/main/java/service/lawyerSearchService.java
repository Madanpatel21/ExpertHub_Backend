package service;

import java.util.List;

import org.springframework.stereotype.Service;

import Repository.lawyerProfileRepository;
import entity.lawyerProfile;

@Service
public class lawyerSearchService {
    private final lawyerProfileRepository LawyerProfileRepository;

    public lawyerSearchService(lawyerProfileRepository LawyerProfileRepository) {
        this.LawyerProfileRepository = LawyerProfileRepository;
    }

    public List<lawyerProfile> search(
        String city,
        String practiceArea,
        Integer minExperience ) {
        return LawyerProfileRepository.searchLawyers(city, practiceArea, minExperience);

    }
}
