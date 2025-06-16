package est.secretary.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import est.secretary.domain.Member;
import est.secretary.domain.Schedule;
import est.secretary.dto.GeminiParseResult;
import est.secretary.dto.ScheduleCountResponse;
import est.secretary.dto.ScheduleRequest;
import est.secretary.dto.ScheduleResponse;
import est.secretary.dto.VoiceAnalysisResult;
import est.secretary.repository.MemberRepository;
import est.secretary.repository.ScheduleRepository;
import jakarta.mail.internet.MimeMessage;
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

	public boolean hasSchedules(Member member) {
		List<Schedule> schedules = scheduleRepository.findByMember(member);
		return !schedules.isEmpty();
	}

	@Transactional
	public void saveScheduleByParsedResult(VoiceAnalysisResult result, Member member) {
		LocalDateTime start = LocalDateTime.parse(result.getStart());

		ScheduleRequest req = new ScheduleRequest();
		req.setTitle(result.getTitle());
		req.setContent(result.getTitle() + " 일정입니다.");
		req.setStart(start);
		req.setEnd(start.plusHours(1));
		req.setLocation(result.getLocation());

		saveSchedule(req, member);
	}

	public String deleteScheduleByParsedResult(VoiceAnalysisResult result, Member member) {
		List<Schedule> matched = scheduleRepository.findByMemberId(member.getId()).stream()
			.filter(s -> {
				boolean titleMatches = result.getTitle() != null && s.getTitle().contains(result.getTitle());
				boolean dateMatches = result.getStart() != null &&
					s.getStart().toLocalDate().toString().equals(result.getStart().substring(0, 10));
				return titleMatches && dateMatches;
			})
			.toList();

		if (matched.isEmpty()) {
			return "❌ 해당 조건과 일치하는 일정이 없어요.";
		} else if (matched.size() > 1) {
			StringBuilder sb = new StringBuilder("⚠️ 여러 일정이 일치해요. 더 구체적으로 말씀해주세요:\n");
			for (Schedule s : matched) {
				sb.append("- ").append(s.getTitle()).append(" / ").append(s.getStart()).append("\n");
			}
			return sb.toString();
		}

		Schedule toDelete = matched.get(0);
		scheduleRepository.delete(toDelete);
		return "🗑️ '" + toDelete.getTitle() + "' 일정이 삭제되었어요.";
	}

	public List<String> findMatchingSchedules(VoiceAnalysisResult criteria, Member member) {
		String title = criteria.getTitle();
		String startCriterion = criteria.getStart();

		List<Schedule> foundSchedules;
		if (startCriterion.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
			LocalDate date = LocalDate.parse(startCriterion);
			LocalDateTime startOfDay = date.atStartOfDay(); // 2025-06-18T00:00:00
			LocalDateTime endOfDay = date.atTime(23, 59, 59);
			foundSchedules = scheduleRepository.findAllByMemberAndTitleAndStartBetween(member, title, startOfDay,
				endOfDay);

		} else {
			LocalDateTime exactDateTime = LocalDateTime.parse(startCriterion);
			foundSchedules = scheduleRepository.findAllByMemberAndTitleAndStart(member, title, exactDateTime);
		}
		return foundSchedules.stream()
			.map(schedule -> "제목: " + schedule.getTitle() + ", 시간: " + schedule.getStart().toString())
			.collect(Collectors.toList());
	}

	public GeminiParseResult parseWithGemini(String query) {
		try {
			String apiKey = "AIzaSyBfBp1LVDJreSb5BhDuJLwV8P7rezGs3XY";
			String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
				+ apiKey;

			String prompt = "다음 문장에서 일정에 대한 title, start(ISO 형식), location을 추출해줘. 누락된 경우 null로 표시하고, json 형식으로 줘:\n" +
				query;

			HttpClient client = HttpClient.newHttpClient();
			HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("""
					{
					    "contents": [{
					        "parts": [{
					            "text": "%s"
					        }]
					    }]
					}
					""".formatted(prompt)))
				.build();

			HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

			JSONObject json = new JSONObject(res.body());
			String content = json.getJSONArray("candidates")
				.getJSONObject(0)
				.getJSONObject("content")
				.getJSONArray("parts")
				.getJSONObject(0)
				.getString("text");

			JSONObject parsed = new JSONObject(content);
			return new GeminiParseResult(
				parsed.optString("title", null),
				parsed.optString("start", null),
				parsed.optString("location", null)
			);

		} catch (Exception e) {
			throw new RuntimeException("Gemini API 실패", e);
		}
	}

	@Transactional
	public void createFromParsed(GeminiParseResult parsed, Member member) {
		ScheduleRequest req = new ScheduleRequest();
		req.setTitle(parsed.getTitle());
		req.setLocation(parsed.getLocation());
		req.setStart(LocalDateTime.parse(parsed.getStart()));
		req.setEnd(req.getStart().plusHours(1));
		req.setContent(parsed.getTitle() + " 일정입니다.");
		saveSchedule(req, member);
	}

	@Transactional(readOnly = true)
	public List<Schedule> findSchedulesByDate(String dateString, Member member) {
		LocalDate date = LocalDate.parse(dateString);
		LocalDateTime startOfDay = date.atStartOfDay();
		LocalDateTime endOfDay = date.atTime(23, 59, 59);
		return scheduleRepository.findAllByMemberAndStartBetween(member, startOfDay, endOfDay);
	}

	public void deleteScheduleById(Long scheduleId, Member member) {
		Schedule schedule = scheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new RuntimeException("삭제할 일정을 찾을 수 없습니다. ID: " + scheduleId));

		if (!schedule.getMember().getId().equals(member.getId())) {
			throw new RuntimeException("일정을 삭제할 권한이 없습니다.");
		}
		scheduleRepository.deleteById(scheduleId);
	}

	@Transactional(readOnly = true)
	public List<Schedule> findSchedulesByDateAndOptionalTitle(String dateString, String title, Member member) {
		LocalDate date = LocalDate.parse(dateString);
		LocalDateTime startOfDay = date.atStartOfDay();
		LocalDateTime endOfDay = date.atTime(23, 59, 59);

		if (title == null || title.isBlank()) {
			return scheduleRepository.findAllByMemberAndStartBetween(member, startOfDay, endOfDay);
		} else {
			return scheduleRepository.findAllByMemberAndTitleContainingAndStartBetween(member, title, startOfDay,
				endOfDay);
		}
	}

	@Transactional(readOnly = true)
	public Schedule findScheduleById(Long scheduleId, Member member) {
		Schedule schedule = scheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new RuntimeException("수정할 일정을 찾을 수 없습니다. ID: " + scheduleId));

		if (!schedule.getMember().getId().equals(member.getId())) {
			throw new RuntimeException("일정을 수정할 권한이 없습니다.");
		}
		return schedule;
	}

	public void save(Schedule schedule) {
		scheduleRepository.save(schedule);
	}

	public List<Schedule> findSchedulesForToday(Member member) {
		LocalDateTime startOfDay = LocalDate.now().atStartOfDay(); // 오늘 00:00:00
		LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX); // 오늘 23:59:59
		return scheduleRepository.findByMemberAndStartBetween(member, startOfDay, endOfDay);
	}

	public List<Schedule> findSchedulesForTodayAndTomorrow(Member member) {
		LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
		LocalDateTime endOfTomorrow = LocalDate.now().plusDays(1).atTime(LocalTime.MAX);

		return scheduleRepository.findByMemberAndStartBetween(member, startOfToday, endOfTomorrow);
	}

}
