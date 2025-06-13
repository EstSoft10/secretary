package est.secretary.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import est.secretary.dto.PromptItem;
import est.secretary.repository.PromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledPromptCacheService {

	private final PromptRepository promptRepository;
	private final PromptCacheService promptCacheService;

	@Scheduled(cron = "0 0 6,12,18,0 * * *", zone = "Asia/Seoul")
	public void warmUpPromptCache() {
		log.info("프롬프트 캐시 시작");
		runCacheJob();
	}

	private void runCacheJob() {
		List<PromptItem> prompts = promptRepository.findAllPrompts();
		int success = 0, fail = 0;
		long start = System.currentTimeMillis();

		for (PromptItem prompt : prompts) {
			String query = prompt.getText();
			try {
				promptCacheService.fetchAndCache(query);
				success++;
			} catch (Exception e) {
				fail++;
			}
		}

		long time = System.currentTimeMillis() - start;
		log.info("프롬프트 캐시 완료 | 총: {}개 | 성공: {} | 실패: {} | 소요: {}ms",
			prompts.size(), success, fail, time);
	}
}
