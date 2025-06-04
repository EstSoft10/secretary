package est.secretary.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import est.secretary.service.ScheduleService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SchedulePageController {

	private final ScheduleService scheduleService;

	// 전체 캘린더 뷰
	@GetMapping("/schedules")
	public String schedulePage() {
		return "calendar";
	}

	@GetMapping("/calendar")
	public String calendarPage() {
		return "calendar";
	}

}
