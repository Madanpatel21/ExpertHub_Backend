package Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import entity.City;
import entity.Country;

@Repository
public interface CountryRepository extends JpaRepository<Country, Integer> {

	@Query(value = "SELECT *  FROM country", nativeQuery = true)
	List<Country> findCountry();

}