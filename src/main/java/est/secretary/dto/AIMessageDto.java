package est.secretary.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AIMessageDto {
	private String sender;
	private String message;
	private LocalDateTime createdAt;
}

