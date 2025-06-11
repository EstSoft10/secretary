package est.secretary.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;

import est.secretary.domain.CustomOAuth2User;
import est.secretary.dto.AIConversationDto;
import est.secretary.dto.AIMessageDto;
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
	public Mono<Map<String, Object>> asyncSearch(@AuthenticationPrincipal CustomOAuth2User principal,
		@RequestParam String query, @RequestParam(required = false) Long conversationId) {
		Long userId = principal != null ? principal.getMember().getId() : null;
		if (userId == null) {
			return callApi(query).map(res -> Map.of("content", res));
		}
		if (conversationId != null) {
			return callApi(query).map(res -> {
				conversationService.addMessage(conversationId, query, res);
				return Map.of("content", res, "conversationId", conversationId);
			});
		} else {
			return callApi(query).map(res -> {
				Long newId = conversationService.createConversation(userId, query, query, res).getId();
				return Map.of("content", res, "conversationId", newId);
			});
		}
	}

	private Mono<String> callApi(String query) {
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
			.doOnError(e -> log.error("WebClient 호출 에러", e))
			.onErrorReturn("서버 응답이 지연되거나 실패했습니다. 잠시 후 다시 시도해주세요.");
	}

	@GetMapping("/conversation/{userId}")
	@ResponseBody
	public List<AIConversationDto> getAllConversations(@PathVariable Long userId) {
		return conversationService.getConversations(userId);
	}

	@GetMapping("/conversation/detail/{conversationId}")
	@ResponseBody
	public List<AIMessageDto> getMessages(@PathVariable Long conversationId) {
		return conversationService.getMessages(conversationId);
	}

	@DeleteMapping("/conversation/{conversationId}")
	@ResponseBody
	public ResponseEntity<?> deleteConversation(@PathVariable Long conversationId) {
		conversationService.deleteConversation(conversationId);
		return ResponseEntity.ok().build();
	}
}
