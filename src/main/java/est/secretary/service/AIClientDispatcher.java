package est.secretary.service;

import java.time.Duration;
import java.util.List;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import est.secretary.configuration.ClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class AIClientDispatcher {

	private final WebClient webClient;
	private final ClientProperties clientProperties;

	private int index = 0;

	public synchronized String queryWithRotation(String query) {
		List<String> clientIds = clientProperties.getIds();
		if (clientIds == null || clientIds.isEmpty()) {
			log.error("client.ids 설정이 비어 있습니다.");
			throw new IllegalStateException("client.ids 설정이 비어 있음");
		}

		String clientId = clientIds.get(index);
		index = (index + 1) % clientIds.size();

		try {
			long start = System.currentTimeMillis();
			String response = callApi(query, clientId);
			long time = System.currentTimeMillis() - start;

			log.info("캐시 성공 | query: \"{}\" | clientId: {} | length: {} | time: {}ms",
				query, clientId, response.length(), time);

			return response;

		} catch (Exception e) {
			log.error("캐시 실패 | query: \"{}\" | clientId: {} | error: {}", query, clientId, e.getMessage());
			throw e;
		}
	}

	private String callApi(String query, String clientId) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder
				.scheme("https")
				.host("kdt-api-function.azurewebsites.net")
				.path("/api/v1/question")
				.queryParam("client_id", clientId)
				.queryParam("content", query)
				.build())
			.retrieve()
			.onStatus(status -> status.value() >= 500 && status.value() < 600, response ->
				response.bodyToMono(String.class)
					.defaultIfEmpty("5xx 에러 응답 없음")
					.flatMap(body -> Mono.error(new RuntimeException(
						"[서버 오류] body=" + body)))
					.cast(Throwable.class)
			)
			.onStatus(HttpStatusCode::is4xxClientError, response ->
				response.bodyToMono(String.class)
					.defaultIfEmpty("4xx 에러 응답 없음")
					.flatMap(body -> Mono.error(new RuntimeException(
						"[클라이언트 오류] status=4xx, body=" + body)))
					.cast(Throwable.class)
			)

			.bodyToMono(String.class)
			.timeout(Duration.ofSeconds(120))
			.block();
	}

}
