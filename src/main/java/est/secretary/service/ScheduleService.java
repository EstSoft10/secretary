package est.secretary.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import est.secretary.domain.Schedule;
import est.secretary.dto.ScheduleCountResponse;
import est.secretary.dto.ScheduleRequest;
import est.secretary.dto.ScheduleResponse;
import est.secretary.repository.MemberRepository;
import est.secretary.repository.ScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ScheduleService {

	private final ScheduleRepository scheduleRepository;
	private final MemberRepository memberRepository;

	public List<ScheduleResponse> getAllSchedules() {
		return scheduleRepository.findAll().stream()
			.map(ScheduleResponse::new)
			.toList();
	}

	public ScheduleResponse getScheduleById(Long id) {
		Schedule schedule = scheduleRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Schedule not found"));

		return new ScheduleResponse(schedule);
	}

	public void saveSchedule(ScheduleRequest request) {
		Schedule schedule = Schedule.builder()
			.title(request.getTitle())
			.content(request.getContent())
			.start(request.getStart())
			.end(request.getEnd())
			.location(request.getLocation())
			.build();

		scheduleRepository.save(schedule);
	}

	@Transactional
	public void updateSchedule(Long id, ScheduleRequest request) {
		Schedule schedule = scheduleRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Schedule not found"));

		schedule.update(
			request.getTitle(),
			request.getContent(),
			request.getStart(),
			request.getEnd(),
			request.getLocation()
		);
	}

	public void deleteSchedule(Long id) {
		scheduleRepository.deleteById(id);
	}

	// 날짜 범위 조회
	public List<ScheduleResponse> getSchedulesBetween(LocalDateTime start, LocalDateTime end) {
		return scheduleRepository.findByStartBetween(start, end).stream()
			.map(ScheduleResponse::new)
			.toList();
	}

	// 특정 일자 조회
	public List<ScheduleResponse> getSchedulesByDate(LocalDate date) {
		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.atTime(LocalTime.MAX);
		return getSchedulesBetween(start, end);
	}

	// 날짜별 일정
	public List<ScheduleCountResponse> getScheduleCountsByDay(LocalDate start, LocalDate end) {
		List<LocalDate> dates = start.datesUntil(end.plusDays(1)).collect(Collectors.toList());

		return dates.stream()
			.map(date -> {
				LocalDateTime dayStart = date.atStartOfDay();
				LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
				int cnt = scheduleRepository.countByDateRange(dayStart, dayEnd);
				return new ScheduleCountResponse(date, cnt);
			})
			.filter(resp -> resp.getCount() > 0)
			.collect(Collectors.toList());
	}
}
