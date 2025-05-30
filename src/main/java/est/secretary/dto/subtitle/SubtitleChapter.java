package est.secretary.dto.subtitle;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleChapter {
	private int chapter_idx;
	private String chapter_title;
	private List<SubtitleLine> text;
}
