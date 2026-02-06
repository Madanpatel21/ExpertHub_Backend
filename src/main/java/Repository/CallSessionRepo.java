package Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import entity.CallSession;

@Repository
public interface CallSessionRepo extends CrudRepository<CallSession, Long> {

    Optional<CallSession> findById(Long id);

    List<CallSession> findByCallerIdOrderByStartTimeDesc(String callerId);

    List<CallSession> findByCalleeIdOrderByStartTimeDesc(String calleeId);

    @Query("SELECT c FROM CallSession c WHERE c.callerId = :userId OR c.calleeId = :userId ORDER BY c.startTime DESC")
    List<CallSession> findAllByUserId(@Param("userId") String userId);

    @Query("SELECT c FROM CallSession c WHERE c.callerId = :userId OR c.calleeId = :userId AND c.callType = :callType ORDER BY c.startTime DESC")
    List<CallSession> findByUserIdAndCallType(@Param("userId") String userId, CallSession.CallType callType);

    @Query("SELECT SUM(c.durationInSeconds) FROM CallSession c WHERE c.callerId = :userId OR c.calleeId = :userId")
    Long getTotalCallDurationByUser(@Param("userId") String userId);

    @Query("SELECT SUM(c.durationInSeconds) FROM CallSession c WHERE c.callType = :callType")
    Long getTotalCallDurationByType(@Param("callType") CallSession.CallType callType);
}
