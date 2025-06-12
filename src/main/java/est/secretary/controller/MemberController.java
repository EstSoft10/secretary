package est.secretary.controller;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import est.secretary.domain.CustomOAuth2User;
import est.secretary.domain.Member;
import est.secretary.dto.PromptItem;
import est.secretary.dto.ScheduleCountResponse;
import est.secretary.service.ScheduleService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MemberController {

	private final ScheduleService scheduleService;

	@GetMapping("/")
	public String index(@AuthenticationPrincipal CustomOAuth2User principal, Model model) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		InputStream inputStream = new ClassPathResource("static/json/prompts.json").getInputStream();
		List<PromptItem> prompts = mapper.readValue(inputStream, new TypeReference<>() {
		});
		Collections.shuffle(prompts);
		model.addAttribute("promptList", prompts.subList(0, Math.min(3, prompts.size())));

		if (principal != null) {
			Member member = principal.getMember();
			model.addAttribute("userName", principal.getName());
			model.addAttribute("profileImage", principal.getProfileImage());

			LocalDate today = LocalDate.now();
			LocalDate start = today.minusDays(3);
			LocalDate end = today.plusDays(3);

			List<ScheduleCountResponse> weekSchedules = scheduleService.getWeeklySchedulesWithDetails(start, end,
				member);
			model.addAttribute("weekSchedules", weekSchedules);
		}

		return "index";
	}

	@GetMapping("/profile")
	public String profilePage(@AuthenticationPrincipal CustomOAuth2User principal, Model model) {
		model.addAttribute("user", principal);
		return "profile";
	}
}
