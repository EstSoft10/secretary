package est.secretary.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import est.secretary.domain.Member;
import est.secretary.domain.Schedule;
import est.secretary.dto.ScheduleCountResponse;
import est.secretary.dto.ScheduleRequest;
import est.secretary.dto.ScheduleResponse;
import est.secretary.repository.MemberRepository;
import est.secretary.repository.ScheduleRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ScheduleService {

	private final ScheduleRepository scheduleRepository;
	private final MemberRepository memberRepository;
	private final JavaMailSender mailSender;

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

	@Transactional
	public void importFromIcs(MultipartFile icsFile, Long userId) {
		try (InputStream in = icsFile.getInputStream()) {
			var calendar = Biweekly.parse(in).first();

			if (calendar == null || calendar.getEvents().isEmpty()) {
				throw new IllegalArgumentException("ICS 파일에 일정 정보가 없습니다.");
			}

			Member member = memberRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

			for (VEvent event : calendar.getEvents()) {
				String title = event.getSummary() != null ? event.getSummary().getValue() : "제목 없음";
				String location = event.getLocation() != null ? event.getLocation().getValue() : "";
				String description = event.getDescription() != null ? event.getDescription().getValue() : "";

				Instant startInstant = event.getDateStart().getValue().toInstant();
				Instant endInstant = event.getDateEnd() != null
					? event.getDateEnd().getValue().toInstant()
					: startInstant.plusSeconds(3600);

				Schedule schedule = Schedule.builder()
					.member(member)
					.title(title)
					.content(description)
					.start(LocalDateTime.ofInstant(startInstant, ZoneId.systemDefault()))
					.end(LocalDateTime.ofInstant(endInstant, ZoneId.systemDefault()))
					.location(location)
					.build();

				scheduleRepository.save(schedule);
			}

		} catch (Exception e) {
			throw new RuntimeException("ICS 파일 처리 중 오류 발생", e);
		}
	}

	@Async
	public void exportToIcsAndSendMail(String email, Member member) {
		List<Schedule> schedules = scheduleRepository.findByMember(member);

		ICalendar ical = new ICalendar();
		ical.setProductId("-//Assistant Calendar//iCal4j//EN");

		for (Schedule s : schedules) {
			VEvent event = new VEvent();
			event.setSummary(s.getTitle());
			event.setDescription(s.getContent());
			event.setDateStart(java.util.Date.from(s.getStart().atZone(ZoneId.systemDefault()).toInstant()));
			event.setDateEnd(java.util.Date.from(s.getEnd().atZone(ZoneId.systemDefault()).toInstant()));
			event.setLocation(s.getLocation());
			ical.addEvent(event);
		}

		try {
			File file = File.createTempFile("schedule_", ".ics");
			try (FileOutputStream fos = new FileOutputStream(file)) {
				Biweekly.write(ical).go(fos);
			}

			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setTo(email);
			helper.setSubject("A:ssistant 일정 내보내기");
			helper.setText("첨부된 .ics 파일을 통해 일정을 동기화하세요!");
			helper.addAttachment("schedule.ics", file);
			mailSender.send(message);

		} catch (Exception e) {
			throw new RuntimeException("이메일 전송 실패", e);
		}
	}
}
