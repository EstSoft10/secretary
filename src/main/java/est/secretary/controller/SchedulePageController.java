package est.secretary.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SchedulePageController {

	// 전체 캘린더 뷰
	@GetMapping("/calendar")
	public String schedulePage() {
		return "calendar";
	}

}
