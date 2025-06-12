package est.secretary.controller;

import java.io.InputStream;
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
import est.secretary.dto.PromptItem;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MemberController {

	@GetMapping("/")
	public String index(@AuthenticationPrincipal CustomOAuth2User principal, Model model) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		InputStream inputStream = new ClassPathResource("static/json/prompts.json").getInputStream();

		List<PromptItem> prompts = mapper.readValue(inputStream, new TypeReference<>() {
		});
		Collections.shuffle(prompts);
		model.addAttribute("promptList", prompts.subList(0, Math.min(3, prompts.size())));

		if (principal != null) {
			model.addAttribute("userName", principal.getName());
			model.addAttribute("profileImage", principal.getProfileImage());
			System.out.println("로그인 사용자: " + principal.getName());
		} else {
			System.out.println("로그인 안 됨");
		}

		return "index";
	}

	@GetMapping("/profile")
	public String profilePage(@AuthenticationPrincipal CustomOAuth2User principal, Model model) {
		model.addAttribute("user", principal);
		return "profile";
	}
}
