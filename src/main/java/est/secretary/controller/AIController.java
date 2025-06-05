package est.secretary.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;

import est.secretary.domain.AIConversation;
import est.secretary.domain.AIMessage;
import est.secretary.service.AIConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
public class AIController {

	private final WebClient webClient;
	private final AIConversationService conversationService;

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
	public Mono<String> asyncSearch(@RequestParam(required = false) Long userId, @RequestParam String query) {
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
			.doOnNext(res -> {
				log.info("응답 성공: {}", res);
				conversationService.createConversation(1L, query, res);
			})
			.doOnError(e -> log.error("WebClient 호출 에러", e))
			.onErrorReturn("서버 응답이 지연되거나 실패했습니다. 잠시 후 다시 시도해주세요.");
	}

	@GetMapping("/conversation/{userId}")
	@ResponseBody
	public List<AIConversation> getAllConversations(@PathVariable Long userId) {
		return conversationService.getConversations(userId);
	}

	@GetMapping("/conversation/detail/{conversationId}")
	@ResponseBody
	public List<AIMessage> getMessages(@PathVariable Long conversationId) {
		return conversationService.getMessages(conversationId);
	}

	@DeleteMapping("/conversation/{conversationId}")
	@ResponseBody
	public ResponseEntity<?> deleteConversation(@PathVariable Long conversationId) {
		conversationService.deleteConversation(conversationId);
		return ResponseEntity.ok().build();
	}
}
