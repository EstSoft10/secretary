package est.secretary.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import est.secretary.domain.Member;
import est.secretary.domain.Schedule;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
	List<Schedule> findByMember(Member member);

	List<Schedule> findByMemberAndStartBetween(Member member, LocalDateTime start, LocalDateTime end);

	@Query("SELECT COUNT(s) FROM Schedule s WHERE s.member = :member AND s.start >= :dayStart AND s.start <= :dayEnd")
	int countByMemberAndDateRange(
		@Param("member") Member member,
		@Param("dayStart") LocalDateTime dayStart,
		@Param("dayEnd") LocalDateTime dayEnd
	);

	Optional<Schedule> findByScheduleIdAndMember(Long scheduleId, Member member);

}
