package est.secretary.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ScheduleCountResponse {
	private LocalDate date;
	private int count;
}
