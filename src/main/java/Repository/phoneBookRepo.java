package Repository;


import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import entity.PhoneBook;

import jakarta.transaction.Transactional;

@Repository
public interface phoneBookRepo extends CrudRepository<PhoneBook,Long> {
	
	

	

	@Query(value = "SELECT * FROM vivah.phonebook WHERE user_id = :userId AND contact_view_id = :contactViewId and phone_number Is NOT  NULL", nativeQuery = true)
	PhoneBook findByUserIdAndContactViewId(@Param("userId") String userId,@Param("contactViewId") String contactViewId);

	
	@Query(value = "SELECT * FROM vivah.phonebook WHERE user_id = :userId AND contact_view_id = :contactViewId and phone_number IS  NULL", nativeQuery = true)
	PhoneBook findByUserIdAlternativeDetails(@Param("userId") String userId,@Param("contactViewId") String contactViewId);


	@Query(value = "SELECT * FROM vivah.phonebook WHERE user_id = :userId AND contact_view_id = :contactViewId and phone_number Is NOT  NULL", nativeQuery = true)
	PhoneBook findByUserIdAndContactViewIds(@Param("userId") String userId,@Param("contactViewId") String contactViewId);
	
	
	@Modifying
	@Transactional
	@Query(value = "DELETE FROM phonebook WHERE user_id = :userId AND contact_view_id = :contactViewId AND phone_number IS NOT NULL", nativeQuery = true)
	int deletePhoneBook(@Param("userId") String userId, @Param("contactViewId") String contactViewId);

	
	@Modifying
	@Transactional
	@Query(value = "DELETE FROM phonebook WHERE user_id = :userId AND contact_view_id = :contactViewId and phone_number Is NULL", nativeQuery = true)
	void deleteAlternativeDetails(@Param("userId") String userId,@Param("contactViewId") String contactViewId);
	
	

}