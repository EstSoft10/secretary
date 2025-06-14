package est.secretary.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

	@GetMapping("/searchResult")
	public String searchResultPage() {
		return "searchResult";
	}

	@GetMapping("/login")
	public String loginPage() {
		return "login";
	}

	@GetMapping("/calendar")
	public String schedulePage() {
		return "calendar";
	}
}
