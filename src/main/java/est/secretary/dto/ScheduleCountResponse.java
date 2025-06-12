package est.secretary.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCountResponse {
	private LocalDate date;
	private int count;

	private List<ScheduleResponse> schedules;

	public ScheduleCountResponse(LocalDate date, int count) {
		this.date = date;
		this.count = count;
	}
}
