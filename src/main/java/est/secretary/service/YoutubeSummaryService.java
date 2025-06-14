package est.secretary.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import est.secretary.dto.subtitle.SubtitleChapter;
import est.secretary.dto.subtitle.SubtitleResponse;
import est.secretary.dto.subtitle.SubtitleWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeSummaryService {

	private final RestTemplate restTemplate = new RestTemplate();

	public List<SubtitleChapter> extractSubtitles(String url) {
		String extractApi = "http://localhost:8000/extract";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, String> request = Map.of("url", url);
		HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

		try {
			ResponseEntity<SubtitleResponse> response = restTemplate.exchange(
				extractApi, HttpMethod.POST, entity, SubtitleResponse.class
			);
			return response.getBody().getChapters();

		} catch (HttpStatusCodeException e) {
			System.out.println("서버 오류 응답: " + e.getResponseBodyAsString());
			throw new RuntimeException("자막 추출 실패: " + e.getStatusCode());
		}
	}

	public String summarizeSubtitles(SubtitleWrapper wrapper) {
		WebClient webClient = WebClient.builder()
			.baseUrl("https://kdt-api-function.azurewebsites.net")
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.build();

		return webClient.post()
			.uri("/api/v1/summary-youtube")
			.bodyValue(wrapper)
			.retrieve()
			.bodyToMono(String.class)
			.block();
	}

}
