package est.secretary.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import est.secretary.dto.VoiceAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

	@Value("${gemini.api.key}")
	private String apiKey;

	private final ObjectMapper objectMapper;
	private final HttpClient client = HttpClient.newHttpClient();

	public VoiceAnalysisResult analyze(String query) {
		try {
			String endpoint =
				"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
					+ apiKey;

			LocalDate today = LocalDate.now();
			String todayString = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
			String tomorrowString = today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

			String systemPrompt = """
				당신은 사용자 음성 명령을 분석해 일정을 등록/수정/삭제/조회하는 AI 비서입니다.
				오늘 날짜는 %s입니다. '내일'은 %s, '모레' 등 상대적인 날짜도 이 기준에 맞춰 'YYYY-MM-DD' 형식으로 변환하세요.
				
				아래 포맷에 맞춰 반드시 JSON으로만 응답하세요.
				값이 없거나 명확하지 않으면 반드시 null로 설정하세요.
				사용자가 '시간 변경해줘' 처럼 값 없이 필드만 말해도, 관련 필드를 모두 null로 설정한 JSON을 반환해야 합니다.
				절대로 JSON 형식이 아닌 일반적인 문장으로 대답하지 마세요.
				
				{
				  "intent": "create | update | delete | read",
				  "title": "일정 제목",
				  "start": "YYYY-MM-DD'T'HH:mm:ss 또는 YYYY-MM-DD 또는 THH:mm:ss",
				  "location": "장소"
				}
				
				---
				[예시 1: 생성]
				사용자 발화: "내일 2시에 엄마랑 병원 가"
				{ "intent": "create", "title": "엄마랑 병원 가기", "start": "%sT14:00:00", "location": null }
				
				[예시 2: 필드만 명시된 수정]
				사용자 발화: "장소만 수정할래"
				{ "intent": "update", "title": null, "start": null, "location": null }
				
				[예시 3: 시간만 명시]
				사용자 발화: "오후 2시"
				{ "intent": null, "title": null, "start": "T14:00:00", "location": null }
				---
				
				실제 사용자 발화: "%s"
				""".formatted(todayString, tomorrowString, tomorrowString, query);

			Map<String, Object> requestBody = Map.of(
				"contents", List.of(
					Map.of("parts", List.of(
						Map.of("text", systemPrompt)
					))
				)
			);

			String jsonRequest = objectMapper.writeValueAsString(requestBody);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(endpoint))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
				.build();

			HttpClient client = HttpClient.newHttpClient();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			String responseBody = response.body();

			JsonNode textNode = objectMapper.readTree(responseBody)
				.path("candidates")
				.path(0)
				.path("content")
				.path("parts")
				.path(0)
				.path("text");

			if (textNode.isMissingNode()) {
				return new VoiceAnalysisResult();
			}
			String text = textNode.asText().trim();

			text = text
				.replaceAll("(?i)^\\s*json\\s*", "")
				.replaceAll("(?s)```json", "")
				.replaceAll("(?s)```", "")
				.replaceAll("^\\s*`+|`+\\s*$", "")
				.trim();

			if (!text.startsWith("{") || !text.endsWith("}")) {
				return new VoiceAnalysisResult();
			}
			VoiceAnalysisResult result = objectMapper.readValue(text, VoiceAnalysisResult.class);
			return result;

		} catch (Exception e) {
			throw new RuntimeException("Gemini API 분석 실패", e);
		}
	}

	public String generateText(String systemPrompt) {
		try {
			String endpoint =
				"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
					+ apiKey;

			Map<String, Object> requestBody = Map.of(
				"contents", List.of(
					Map.of("parts", List.of(
						Map.of("text", systemPrompt)
					))
				)
			);

			String jsonRequest = objectMapper.writeValueAsString(requestBody);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(endpoint))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
				.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			String responseBody = response.body();

			JsonNode textNode = objectMapper.readTree(responseBody)
				.path("candidates")
				.path(0)
				.path("content")
				.path("parts")
				.path(0)
				.path("text");

			if (textNode.isMissingNode() || textNode.isNull()) {
				JsonNode finishReason = objectMapper.readTree(responseBody)
					.path("candidates").path(0).path("finishReason");
				log.error("마무리 사유: {}", finishReason.asText("UNKNOWN"));
				return null;
			}

			return textNode.asText().trim();

		} catch (Exception e) {
			log.error("Gemini 텍스트 생성 중 오류 발생", e);
			return null;
		}
	}
}
