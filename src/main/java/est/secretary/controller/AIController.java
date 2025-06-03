package est.secretary.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
public class AIController {

	private final WebClient webClient;

	@Value("${client.id}")
	private String clientId;

	@GetMapping("/search")
	public String searchRedirect(@RequestParam String query) {
		String lowerQuery = query.toLowerCase();
		if (lowerQuery.contains("youtube") || lowerQuery.contains("유튜브")) {
			return "redirect:/youtube-summary";
		}

		return "redirect:/searchResult?query=" + UriUtils.encode(query, StandardCharsets.UTF_8);
	}

	@GetMapping("/async-search")
	@ResponseBody
	public Mono<String> asyncSearch(@RequestParam String query) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder
				.scheme("https")
				.host("kdt-api-function.azurewebsites.net")
				.path("/api/v1/question")
				.queryParam("client_id", clientId)
				.queryParam("content", query)
				.build())
			.retrieve()
			.bodyToMono(String.class)
			.doOnNext(res -> log.info("응답 성공: {}", res));
	}
}
