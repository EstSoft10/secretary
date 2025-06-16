package est.secretary.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoiceAnalysisResult {
	private String intent;
	private String title;
	private String start;
	private String location;
}
