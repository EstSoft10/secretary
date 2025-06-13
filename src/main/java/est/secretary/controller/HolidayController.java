package est.secretary.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import est.secretary.service.HolidayService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

	private final HolidayService holidayService;

	@GetMapping
	public ResponseEntity<List<Map<String, String>>> getHolidays(
		@RequestParam int year,
		@RequestParam int month) throws IOException {

		return ResponseEntity.ok(holidayService.getHolidayEvents(year, month));
	}
}
