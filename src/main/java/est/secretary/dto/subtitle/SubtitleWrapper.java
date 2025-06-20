package est.secretary.dto.subtitle;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleWrapper {
	private List<SubtitleChapter> subtitle;
}

