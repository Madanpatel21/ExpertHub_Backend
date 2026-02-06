package Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import entity.City;
import entity.Country;


@Repository
public interface CityRepository extends JpaRepository<City, Integer> {


	
	
	@Query(value = "SELECT *  FROM city  WHERE state_id =  :stateId", nativeQuery = true)
	List<City> findStatesByStateId(@Param("stateId") Integer stateId);

	
	@Query(value = "SELECT *  FROM country", nativeQuery = true)
	Country findCountry();

			
	
}