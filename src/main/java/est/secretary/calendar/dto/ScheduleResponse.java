package est.secretary.calendar.dto;

import java.time.LocalDateTime;

import est.secretary.calendar.domain.Schedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleResponse {
	private Long scheduleId;
	private String title;
	private String content;
	private LocalDateTime start;
	private LocalDateTime end;
	private String location;

	@Builder
	public ScheduleResponse(Schedule schedule) {
		this.scheduleId = schedule.getScheduleId();
		this.title = schedule.getTitle();
		this.content = schedule.getContent();
		this.start = schedule.getStart();
		this.end = schedule.getEnd();
		this.location = schedule.getLocation();
	}
}
