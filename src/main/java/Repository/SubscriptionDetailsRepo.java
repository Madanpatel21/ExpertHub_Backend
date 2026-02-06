package Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import entity.SubscriptionDetails;


@Repository
public interface SubscriptionDetailsRepo extends CrudRepository<SubscriptionDetails, Integer>{
	
	@Query(value = "SELECT * FROM subscription_details", nativeQuery = true)
	List<SubscriptionDetails> findDetails();
	
	
	

  @Query(value = "SELECT cupon_code FROM cupon_code WHERE cupon_code = :cuponCode AND cupon_code_value = 1", nativeQuery = true)
  String findCuponCode(String cuponCode);

  @Query(value = "SELECT user_id, cupon_code FROM cupon_code WHERE user_id = :userId AND cupon_code_value = 1", nativeQuery = true)
List<Object[]> findAllCuponCodeWithUserId(@Param("userId") String userId);


}