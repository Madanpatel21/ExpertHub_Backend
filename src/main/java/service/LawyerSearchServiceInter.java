package service;



import java.util.List;

import dto.LawyerSearchDTO;
import entity.lawyerProfile;

public interface LawyerSearchServiceInter {
    List<lawyerProfile> searchAndSortLawyers(LawyerSearchDTO dto);
}