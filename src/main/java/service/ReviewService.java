package service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Repository.AppointmentRepository;
import Repository.lawyerProfileRepository;
import Repository.ReviewRepository;
import dto.ReviewRequest;
import entity.Lawyer_rating;
import entity.Review;
import entity.lawyerProfile;
import entity.user;
import exception.badRequestException;

@Service
public class ReviewService {
	private final ReviewRepository reviewRepo;
	private final lawyerProfileRepository lawyerRepo;
	private final AppointmentRepository appointmentRepo;

	public ReviewService(ReviewRepository reviewRepo, lawyerProfileRepository lawyerRepo,
			AppointmentRepository appointmentRepo) {
		this.reviewRepo = reviewRepo;
		this.lawyerRepo = lawyerRepo;
		this.appointmentRepo = appointmentRepo;
	}

	@Transactional
	@org.springframework.cache.annotation.CacheEvict(value = { "lawyer_reviews",
			"lawyer_rating" }, key = "#request.lawyerId")
	public void submitReview(ReviewRequest request, user client) {

		if (reviewRepo.existsByClientIdAndAppointmentId(
				client.getId(),
				request.getAppointmentId())) {
			throw new badRequestException("Review already submitted");
		}

		Review review = new Review();
		review.setClientId(client.getId());
		review.setLawyerId(request.getLawyerId());
		review.setAppointmentId(request.getAppointmentId());
		review.setRating(request.getRating());
		review.setComment(request.getComment());

		reviewRepo.save(review);

		// UPDATE LAWYER RATING CACHE
		lawyerProfile profile = lawyerRepo.findById(request.getLawyerId())
				.orElseThrow(() -> new badRequestException("Lawyer not found"));

		int newTotal = profile.getTotalReviews() + 1;
		double newAvg = ((profile.getAverageRating() * profile.getTotalReviews())
				+ request.getRating()) / newTotal;

		profile.setTotalReviews(newTotal);
		profile.setAverageRating(newAvg);

		lawyerRepo.save(profile);
	}

	@org.springframework.cache.annotation.Cacheable(value = "lawyer_reviews", key = "#lawyerId")
	public List<Review> getLawyerReviews(Long lawyerId) {
		return reviewRepo.findByLawyerId(lawyerId);
	}

	@org.springframework.cache.annotation.Cacheable(value = "lawyer_rating", key = "#lawyerId")
	public Double getLawyerRating(Long lawyerId) {
		return reviewRepo.getAverageRating(lawyerId);
	}
}
