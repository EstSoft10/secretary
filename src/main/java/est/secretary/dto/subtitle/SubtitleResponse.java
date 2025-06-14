package est.secretary.dto.subtitle;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SubtitleResponse {
	@JsonProperty("subtitle")
	private List<SubtitleChapter> chapters;
}
