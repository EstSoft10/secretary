package est.secretary.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import est.secretary.domain.Schedule;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
	List<Schedule> findByStartBetween(LocalDateTime start, LocalDateTime end);

	@Query("SELECT COUNT(s) FROM Schedule s WHERE s.start >= :dayStart AND s.start <= :dayEnd")
	int countByDateRange(
		@Param("dayStart") LocalDateTime dayStart,
		@Param("dayEnd") LocalDateTime dayEnd
	);
}
