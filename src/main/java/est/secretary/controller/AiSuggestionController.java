package est.secretary.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import est.secretary.domain.CustomOAuth2User;
import est.secretary.domain.Member;
import est.secretary.domain.Schedule;
import est.secretary.dto.AiSuggestion;
import est.secretary.repository.MemberRepository;
import est.secretary.service.AiSuggestionService;
import est.secretary.service.ScheduleService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiSuggestionController {

	private final ScheduleService scheduleService;
	private final AiSuggestionService aiSuggestionService;
	private final MemberRepository memberRepository;

	@GetMapping("/suggestion")
	public ResponseEntity<?> getTodaySuggestion(@AuthenticationPrincipal CustomOAuth2User principal) {
		Member member = memberRepository.findByEmail(principal.getEmail())
			.orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
		List<Schedule> schedules = scheduleService.findSchedulesForTodayAndTomorrow(member);
		List<AiSuggestion> suggestions = aiSuggestionService.getSuggestionsForSchedules(schedules);
		return ResponseEntity.ok(Collections.singletonMap("suggestions", suggestions));
	}
}
