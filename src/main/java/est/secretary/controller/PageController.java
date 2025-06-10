package est.secretary.controller;

import est.secretary.dto.PromptItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

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
}