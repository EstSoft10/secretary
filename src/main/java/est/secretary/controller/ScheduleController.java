package est.secretary.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import est.secretary.domain.CustomOAuth2User;
import est.secretary.domain.Member;
import est.secretary.dto.GeminiParseResult;
import est.secretary.dto.ScheduleCountResponse;
import est.secretary.dto.ScheduleRequest;
import est.secretary.dto.ScheduleResponse;
import est.secretary.dto.VoiceScheduleRequest;
import est.secretary.repository.MemberRepository;
import est.secretary.service.ScheduleService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

	private final ScheduleService scheduleService;
	private final MemberRepository memberRepository;

	// Month 단위 전체 일정 조회
	@GetMapping
	public List<ScheduleResponse> getSchedulesByMonth(
		@AuthenticationPrincipal CustomOAuth2User principal,
		@RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
		@RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
		Member member = getMemberFromPrincipal(principal);
		return scheduleService.getSchedulesBetween(start, end, member);
	}

	// 특정 일자 조회
	@GetMapping("/day")
	public List<ScheduleResponse> getSchedulesByDate(
		@AuthenticationPrincipal CustomOAuth2User principal,
		@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		Member member = getMemberFromPrincipal(principal);
		return scheduleService.getSchedulesByDate(date, member);
	}

	// 일정 생성
	@PostMapping
	public ResponseEntity<Void> createSchedule(
		@AuthenticationPrincipal CustomOAuth2User principal,
		@RequestBody ScheduleRequest request) {
		Member member = getMemberFromPrincipal(principal);
		scheduleService.saveSchedule(request, member);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	// 수정
	@PutMapping("/{id}")
	public ResponseEntity<Void> updateSchedule(
		@AuthenticationPrincipal CustomOAuth2User principal,
		@PathVariable Long id,
		@RequestBody ScheduleRequest request) {
		Member member = getMemberFromPrincipal(principal);
		scheduleService.updateSchedule(id, request, member);
		return ResponseEntity.ok().build();
	}

	// 삭제
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteSchedule(
		@AuthenticationPrincipal CustomOAuth2User principal,
		@PathVariable Long id) {
		Member member = getMemberFromPrincipal(principal);
		scheduleService.deleteSchedule(id, member);
		return ResponseEntity.noContent().build();
	}

	// 날짜별 일정 개수 조회
	@GetMapping("/counts")
	public List<ScheduleCountResponse> getScheduleCountsByMonth(
		@AuthenticationPrincipal CustomOAuth2User principal,
		@RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
		@RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
		Member member = getMemberFromPrincipal(principal);
		return scheduleService.getScheduleCountsByDay(start.toLocalDate(), end.toLocalDate(), member);
	}

	// 로그인한 사용자 -> Member 객체로 변환
	private Member getMemberFromPrincipal(CustomOAuth2User principal) {
		return memberRepository.findByEmail(principal.getEmail())
			.orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));
	}

	// 일정 단건 조회
	@GetMapping("/{id}")
	public ScheduleResponse getScheduleById(
		@AuthenticationPrincipal CustomOAuth2User principal,
		@PathVariable Long id) {
		Member member = getMemberFromPrincipal(principal);
		return scheduleService.getScheduleById(id, member);
	}

	@PostMapping("/gemini")
	public ResponseEntity<?> handleVoiceByGemini(
		@AuthenticationPrincipal CustomOAuth2User principal,
		@RequestBody VoiceScheduleRequest request
	) {
		Member member = getMemberFromPrincipal(principal);
		GeminiParseResult parsed = scheduleService.parseWithGemini(request.getQuery());

		if (!parsed.isComplete()) {
			return ResponseEntity.ok(Map.of(
				"status", "incomplete",
				"message", parsed.getMissingMessage()
			));
		}

		scheduleService.createFromParsed(parsed, member);

		return ResponseEntity.ok(Map.of(
			"status", "success",
			"title", parsed.getTitle(),
			"start", parsed.getStart().toString(),
			"location", parsed.getLocation()
		));
	}

}
