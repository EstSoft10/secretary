package est.secretary.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import est.secretary.domain.Schedule;
import est.secretary.dto.AiSuggestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiSuggestionService {

	private final GeminiService geminiService;
	private final ObjectMapper objectMapper;

	public List<AiSuggestion> getSuggestionsForSchedules(List<Schedule> schedules) {
		if (schedules == null || schedules.isEmpty()) {
			return Collections.emptyList();
		}

		String scheduleSummary = schedules.stream()
			.map(s -> String.format(
				"제목: %s, 시작시간: %s, 장소: %s",
				s.getTitle(),
				s.getStart().toLocalTime().toString(),
				s.getLocation() != null ? s.getLocation() : "미정"
			))
			.collect(Collectors.joining("\n"));

		String systemPrompt = """
			당신은 사용자의 일정을 분석하여, 유용한 제안을 하는 AI 비서입니다.
			
			[오늘 일정]
			%s
			
			[지침]
			1. 각 중요 일정에 대해, 사용자에게 보여줄 친절한 제안 문구(displayText)와, 해당 제안을 클릭했을 때 내부적으로 실행될 짧은 핵심 명령어(actionQuery)를 생성하세요.
			2. `actionQuery`는 사용자가 직접 말하는 것처럼 간결해야 합니다.
			3. 모든 제안을 JSON 객체 배열로 만드세요. 각 객체는 `displayText`와 `actionQuery` 키를 가져야 합니다.
			4. 응답은 오직 JSON 객체만 포함해야 합니다. 다른 어떤 설명도 덧붙이지 마세요.
			5. 유용하거나 창의적인 제안을 생성하세요.
			
			[응답 형식]
			{
			  "suggestions": [
			    {
			      "displayText": "오늘 저녁 강남역에서 데이트가 있으시네요! 근처 맛집을 추천해드릴까요?",
			      "actionQuery": "강남역 맛집 추천"
			    },
			    {
			      "displayText": "내일 오전에 회사에서 미팅이 있으시네요! 미팅 장소 알아볼까요?",
			      "actionQuery": "공간대여서비스 확인"
			    },
			    {
			      "displayText": "발표 준비는 잘 되셨나요? 발표 후에 나올 만한 예상 질문 리스트를 함께 만들어볼까요?",
			      "actionQuery": "발표 예상 질문 만들어줘"
			    }
			  ]
			}
			
			제안할 내용이 없다면 `{"suggestions": []}`를 반환하세요.
			""".formatted(scheduleSummary);

		try {
			String jsonResponse = geminiService.generateText(systemPrompt);
			String sanitizedJson = jsonResponse.replaceAll("(?s)```json", "").replaceAll("(?s)```", "").trim();

			Map<String, List<AiSuggestion>> result = objectMapper.readValue(sanitizedJson, new TypeReference<>() {
			});
			return result.getOrDefault("suggestions", Collections.emptyList());

		} catch (Exception e) {
			log.error("AI 추천 메시지 JSON 파싱 실패", e);
			return Collections.emptyList();
		}
	}
}
