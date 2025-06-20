package est.secretary.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleRequest {
	private String title;
	private String content;
	private LocalDateTime start;
	private LocalDateTime end;
	private String location;
}
