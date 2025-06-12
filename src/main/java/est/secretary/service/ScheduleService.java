package est.secretary.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import est.secretary.domain.Member;
import est.secretary.domain.Schedule;
import est.secretary.dto.ScheduleCountResponse;
import est.secretary.dto.ScheduleRequest;
import est.secretary.dto.ScheduleResponse;
import est.secretary.repository.ScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ScheduleService {

	private final ScheduleRepository scheduleRepository;

	@Transactional
	public void saveSchedule(ScheduleRequest request, Member member) {
		Schedule schedule = Schedule.builder()
			.title(request.getTitle())
			.content(request.getContent())
			.start(request.getStart())
			.end(request.getEnd())
			.location(request.getLocation())
			.member(member)
			.build();
		scheduleRepository.save(schedule);
	}

	@Transactional
	public void updateSchedule(Long id, ScheduleRequest request, Member member) {
		Schedule schedule = scheduleRepository.findById(id)
			.filter(s -> s.getMember().getId().equals(member.getId()))
			.orElseThrow(() -> new RuntimeException("해당 사용자의 일정이 존재하지 않습니다."));

		schedule.update(
			request.getTitle(),
			request.getContent(),
			request.getStart(),
			request.getEnd(),
			request.getLocation()
		);
	}

	@Transactional
	public void deleteSchedule(Long id, Member member) {
		Schedule schedule = scheduleRepository.findById(id)
			.filter(s -> s.getMember().getId().equals(member.getId()))
			.orElseThrow(() -> new RuntimeException("해당 사용자의 일정이 존재하지 않습니다."));

		scheduleRepository.delete(schedule);
	}

	// 날짜 범위 조회
	public List<ScheduleResponse> getSchedulesBetween(LocalDateTime start, LocalDateTime end, Member member) {
		return scheduleRepository.findByMemberAndStartBetween(member, start, end).stream()
			.map(ScheduleResponse::new)
			.toList();
	}

	// 특정 일자 조회
	public List<ScheduleResponse> getSchedulesByDate(LocalDate date, Member member) {
		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.atTime(LocalTime.MAX);
		return getSchedulesBetween(start, end, member);
	}

	// 날짜별 일정
	public List<ScheduleCountResponse> getScheduleCountsByDay(LocalDate start, LocalDate end, Member member) {
		List<LocalDate> dates = start.datesUntil(end.plusDays(1)).collect(Collectors.toList());

		return dates.stream()
			.map(date -> {
				LocalDateTime dayStart = date.atStartOfDay();
				LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
				int count = scheduleRepository.countByMemberAndDateRange(member, dayStart, dayEnd);
				return new ScheduleCountResponse(date, count);
			})
			.filter(resp -> resp.getCount() > 0)
			.toList();
	}

	public List<ScheduleCountResponse> getWeeklySchedulesWithDetails(LocalDate start, LocalDate end, Member member) {
		List<LocalDate> dates = start.datesUntil(end.plusDays(1)).collect(Collectors.toList());

		return dates.stream()
			.map(date -> {
				LocalDateTime dayStart = date.atStartOfDay();
				LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

				List<ScheduleResponse> scheduleList = scheduleRepository.findByMemberAndStartBetween(member, dayStart,
						dayEnd)
					.stream()
					.map(ScheduleResponse::new)
					.toList();

				return new ScheduleCountResponse(date, scheduleList.size(), scheduleList);
			})
			.toList();
	}

	public ScheduleResponse getScheduleById(Long id, Member member) {
		Schedule schedule = scheduleRepository.findByScheduleIdAndMember(id, member)
			.orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));
		return new ScheduleResponse(schedule);
	}

}
