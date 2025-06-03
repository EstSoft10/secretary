package est.secretary.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import est.secretary.service.ScheduleService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/schedules")
public class SchedulePageController {

	private final ScheduleService scheduleService;

	// 전체 캘린더 뷰
	@GetMapping
	public String schedulePage() {
		return "calendar";
	}

}
