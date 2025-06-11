package est.secretary.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AIConversationDto {
	private Long id;
	private String title;
	private LocalDateTime updatedAt;
}
