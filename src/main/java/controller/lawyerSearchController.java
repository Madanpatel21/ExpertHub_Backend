// package controller;

// import java.util.List;

// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;

// import dto.apiResponse;
// import entity.lawyerProfile;
// import service.lawyerSearchService;

// @RestController
// @RequestMapping("/api/lawyers")
// public class lawyerSearchController {
//     private final lawyerSearchService searchService;

//     public lawyerSearchController(lawyerSearchService searchService) {
//         this.searchService = searchService;
//     }
//     @GetMapping("/search")
//     public apiResponse<List<lawyerProfile>> searchLawyers(
//         @RequestParam String city,
//         @RequestParam String practiceArea,
//         @RequestParam Integer minExperience) {
//         return new apiResponse<>(
//             "Lawyers retrieved successfully",
//              true,
//              searchService.search(city, practiceArea, minExperience)
//         );
//     }

// }

package controller;

import lombok.RequiredArgsConstructor;
import service.LawyerSearchServiceImp;

import org.springframework.web.bind.annotation.*;

import Repository.lawyerProfileRepository;
import dto.LawyerSearchDTO;
import dto.apiResponse;
import entity.lawyerProfile;

import java.util.List;

@RestController
@RequestMapping("/api/lawyers")
@RequiredArgsConstructor
public class lawyerSearchController {

    private final LawyerSearchServiceImp lawyerSearchService;
    private final lawyerProfileRepository lawyerRepo;

    @PostMapping("/search")
    public apiResponse<List<lawyerProfile>> searchLawyers(@RequestBody LawyerSearchDTO dto) {
        List<lawyerProfile> results = lawyerSearchService.searchAndSortLawyers(dto);
        return new apiResponse<>("Lawyers retrieved successfully", true, results);
    }

    @GetMapping("/{id}")
    @org.springframework.cache.annotation.Cacheable(value = "lawyer_details", key = "#id")
    public apiResponse<lawyerProfile> getLawyerDetails(@PathVariable Long id) {
        lawyerProfile profile = lawyerRepo.findById(id)
                .orElseThrow(() -> new exception.badRequestException("Lawyer not found"));
        return new apiResponse<>("Lawyer details fetched successfully", true, profile);
    }
}
