package controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Util.securityUtil;
import dto.ReviewRequest;
import dto.apiResponse;
import entity.Review;
import entity.user;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import service.ReviewService;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
	private final ReviewService reviewService;
	private final securityUtil securityUtil;

    public ReviewController(ReviewService reviewService, securityUtil securityUtil) {
        this.reviewService = reviewService;
        this.securityUtil =securityUtil;
    }

    @PostMapping
    public apiResponse<String> submitReview(
            @RequestBody ReviewRequest request) {

        user client = securityUtil.getCurrentUser();

        reviewService.submitReview(request, client);

        return new apiResponse<>("Review submitted", true, null);
    }

    @GetMapping("/lawyer/{lawyerId}")
    public apiResponse<List<Review>> getReviews(
            @PathVariable Long lawyerId) {

        return new apiResponse<>(
                "Reviews fetched",
                true,
                reviewService.getLawyerReviews(lawyerId)
        );
    }

    @GetMapping("/lawyer/{lawyerId}/rating")
    public apiResponse<Double> getRating(
            @PathVariable Long lawyerId) {

        return new apiResponse<>(
                "Rating fetched",
                true,
                reviewService.getLawyerRating(lawyerId)
        );
    }
}
