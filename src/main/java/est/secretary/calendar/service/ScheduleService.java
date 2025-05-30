package est.secretary.calendar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;

import est.secretary.calendar.domain.Schedule;
import est.secretary.calendar.dto.ScheduleRequest;
import est.secretary.calendar.dto.ScheduleResponse;
import est.secretary.calendar.repository.ScheduleRepository;
import est.secretary.member.Member;
import est.secretary.member.MemberRepository;
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
		Member member = memberRepository.findById(request.getMemberId())
			.orElseThrow(() -> new RuntimeException("Member not found"));

		Schedule schedule = Schedule.builder()
			.member(member)
			.title(request.getTitle())
			.content(request.getContent())
			.start(request.getStart())
			.end(request.getEnd())
			.location(request.getLocation())
			.build();

		scheduleRepository.save(schedule);
	}

	public void updateSchedule(Long id, ScheduleRequest request) {
		Schedule schedule = scheduleRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Schedule not found"));

		Member member = memberRepository.findById(request.getMemberId())
			.orElseThrow(() -> new RuntimeException("Member not found"));

		schedule.update(
			member,
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
}
