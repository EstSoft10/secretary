package est.secretary.service;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromptCacheService {

	private final AIClientDispatcher aiClientDispatcher;
	private final CacheManager cacheManager;

	public String fetchAndCache(String query) {
		String answer = aiClientDispatcher.queryWithRotation(query);
		cacheManager.getCache("promptCache").put(query, answer);
		return answer;
	}

	@Cacheable(value = "promptCache", key = "#query")
	public String getCached(String query) {
		return fetchAndCache(query);
	}
}
