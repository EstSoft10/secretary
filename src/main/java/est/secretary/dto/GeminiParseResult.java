package est.secretary.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeminiParseResult {
	private String title;
	private String start;
	private String location;

	public boolean isComplete() {
		return title != null && start != null && location != null;
	}

	public String getMissingMessage() {
		if (title == null)
			return "무엇을 위한 일정인가요?";
		if (start == null)
			return "언제 일정인가요?";
		if (location == null)
			return "어디에서 진행되나요?";
		return "정보가 부족합니다.";
	}
}
