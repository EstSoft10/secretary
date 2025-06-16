package est.secretary.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiActionService {

	private final GeminiService geminiService;

	public String processAction(String newQuery, List<Map<String, String>> history) {
		if (newQuery == null || newQuery.isBlank()) {
			return "무슨 말씀이신지 잘 모르겠어요.";
		}
		String prompt = createPromptWithHistory(history, newQuery);

		try {
			String response = geminiService.generateText(prompt);
			return response != null ? response : "죄송합니다, 답변을 생성하는 데 문제가 발생했어요.";
		} catch (Exception e) {
			log.error("AI 액션 답변 생성 중 오류 발생: {}", e.getMessage());
			return "죄송합니다, 요청을 처리하는 중 오류가 발생했어요.";
		}
	}

	private String createPromptWithHistory(List<Map<String, String>> history, String newQuery) {
		StringBuilder historyText = new StringBuilder();
		if (history != null && !history.isEmpty()) {
			history.forEach(turn -> {
				historyText.append(String.format("### %s:\n%s\n\n", turn.get("role"), turn.get("content")));
			});
		}

		return String.format("""
			당신은 사용자와 대화하는 AI 비서입니다. 이전 대화 내용을 참고하여 다음 질문에 자연스럽게 답변해주세요.
			
			--- 이전 대화 ---
			%s
			--- 현재 질문 ---
			### user:
			%s
			
			--- 답변 ---
			### model:
			""", historyText.toString(), newQuery);
	}
}
