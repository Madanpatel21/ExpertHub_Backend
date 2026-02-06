package Repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import entity.SmsPojo;

	

@Repository
public interface smsRepository extends CrudRepository<SmsPojo, Long> {

}
