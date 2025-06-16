package est.secretary.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class ScheduleRequest {
	private String title;
	private String content;
	private LocalDateTime start;
	private LocalDateTime end;
	private String location;
}
