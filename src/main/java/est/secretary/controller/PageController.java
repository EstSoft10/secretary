package est.secretary.controller;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import est.secretary.dto.PromptItem;

@Controller
public class PageController {

	@GetMapping("/searchResult")
	public String searchResultPage() {
		return "searchResult";
	}

	@GetMapping("/")
	public String index(Model model) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		InputStream inputStream = new ClassPathResource("static/json/prompts.json").getInputStream();

		List<PromptItem> prompts = mapper.readValue(inputStream, new TypeReference<>() {
		});
		Collections.shuffle(prompts);
		model.addAttribute("promptList", prompts.subList(0, Math.min(3, prompts.size())));
		return "index";
	}

}
