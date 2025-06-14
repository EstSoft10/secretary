package est.secretary.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import est.secretary.dto.subtitle.SubtitleChapter;
import est.secretary.dto.subtitle.SubtitleWrapper;
import est.secretary.service.YoutubeSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class YoutubeSummaryController {

	private final YoutubeSummaryService youtubeSummaryService;

	@PostMapping("/youtube/extract-and-summary")
	public ResponseEntity<?> extractAndSummarize(@RequestBody Map<String, String> request) {
		try {
			String url = request.get("url");
			log.info("유튜브 자막 추출 + 요약 요청: {}", url);

			List<SubtitleChapter> subtitles = youtubeSummaryService.extractSubtitles(url);
			SubtitleWrapper wrapper = new SubtitleWrapper(subtitles);
			String summary = youtubeSummaryService.summarizeSubtitles(wrapper);

			Map<String, Object> response = Map.of(
				"summary", summary,
				"subtitle", subtitles
			);
			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("자막 + 요약 처리 중 오류", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("자막 또는 요약 처리 실패");
		}
	}

	@GetMapping("/youtube-summary")
	public String showYoutubeSummaryPage() {
		return "youtube-summary";
	}

}
