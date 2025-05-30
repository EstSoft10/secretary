package est.secretary.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import est.secretary.dto.ScheduleRequest;
import est.secretary.dto.ScheduleResponse;
import est.secretary.service.ScheduleService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
@RequestMapping("/api/schedules")
public class ScheduleController {

	private final ScheduleService scheduleService;

	// Month 단위 전체 일정 조회
	@GetMapping
	public List<ScheduleResponse> getSchedulesByMonth(
		@RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
		@RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

		return scheduleService.getSchedulesBetween(start, end);
	}

	// 특정 일자 조회
	@GetMapping("/day")
	public List<ScheduleResponse> getSchedulesByDate(
		@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return scheduleService.getSchedulesByDate(date);
	}

	// 일정 생성
	@PostMapping
	public ResponseEntity<Void> createSchedule(@RequestBody ScheduleRequest request) {
		scheduleService.saveSchedule(request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	// 수정
	@PutMapping("/{id}")
	public ResponseEntity<Void> updateSchedule(@PathVariable Long id, @RequestBody ScheduleRequest request) {
		scheduleService.updateSchedule(id, request);
		return ResponseEntity.ok().build();
	}

	// 삭제
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
		scheduleService.deleteSchedule(id);
		return ResponseEntity.noContent().build();
	}
}
