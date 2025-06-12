package est.secretary.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import est.secretary.domain.CustomOAuth2User;
import est.secretary.service.ScheduleService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/schedule")
public class ScheduleSyncController {

	private final ScheduleService scheduleService;

	@PostMapping("/import")
	public String importCalendar(@RequestParam("icsFile") MultipartFile icsFile,
		@AuthenticationPrincipal CustomOAuth2User user) {
		scheduleService.importFromIcs(icsFile, user.getMember().getId());
		return "redirect:/calendar";
	}

	@PostMapping("/export")
	@ResponseBody
	public String exportToEmail(@RequestParam("email") String email,
		@AuthenticationPrincipal CustomOAuth2User user) {
		scheduleService.exportToIcsAndSendMail(email, user.getMember());
		return "ok";
	}

}
