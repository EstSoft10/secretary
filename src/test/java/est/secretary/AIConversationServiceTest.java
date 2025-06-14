package est.secretary;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import est.secretary.domain.AIConversation;
import est.secretary.domain.AIMessage;
import est.secretary.dto.AIConversationDto;
import est.secretary.dto.AIMessageDto;
import est.secretary.repository.AIConversationRepository;
import est.secretary.repository.AIMessageRepository;
import est.secretary.service.AIConversationService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
public class AIConversationServiceTest {

	@Autowired
	private AIConversationService conversationService;

	@Autowired
	private AIConversationRepository conversationRepository;

	@Autowired
	private AIMessageRepository messageRepository;

	@Autowired
	private EntityManager em;

	private static final Long TEST_USER_ID = 1L;

	@Test
	void 질문_응답_저장_후_조회() {
		String query = "반려동물 사료 시장 분석";
		String aiResponse = "{\"action\":{\"name\":\"search_web\"},\"content\":\"테스트 응답 내용입니다.\"}";

		AIConversation conversation = conversationService.createConversation(TEST_USER_ID, query, query, aiResponse);
		List<AIMessageDto> messages = conversationService.getMessages(conversation.getId());

		assertThat(messages).hasSize(2);
		assertThat(messages.get(0).getSender()).isEqualTo(AIMessage.Sender.USER);
		assertThat(messages.get(0).getMessage()).isEqualTo(query);
		assertThat(messages.get(1).getSender()).isEqualTo(AIMessage.Sender.AI);
		assertThat(messages.get(1).getMessage()).isEqualTo(aiResponse);
	}

	@Test
	void 대화_목록_조회() {
		conversationService.createConversation(TEST_USER_ID, "테스트1", "테스트1", "응답1");
		conversationService.createConversation(TEST_USER_ID, "테스트2", "테스트1", "응답2");

		List<AIConversationDto> list = conversationService.getConversations(TEST_USER_ID);
		assertThat(list.size()).isGreaterThanOrEqualTo(2);
	}

	@Test
	void 대화_삭제() {
		AIConversation conversation = conversationService.createConversation(TEST_USER_ID, "삭제할 질문", "삭제할 질문",
			"삭제할 응답");
		Long id = conversation.getId();

		conversationService.deleteConversation(id);
		assertThat(conversationRepository.findById(id)).isEmpty();
		assertThat(messageRepository.findByConversationIdOrderByCreatedAt(id)).isEmpty();
	}

	@Test
	void 메시지_추가시_updatedAt_갱신됨() throws InterruptedException {
		AIConversation conversation = conversationService.createConversation(TEST_USER_ID, "초기 질문", "초기 질문", "초기 응답");
		Long conversationId = conversation.getId();
		LocalDateTime initialUpdatedAt = conversation.getUpdatedAt();
		Thread.sleep(1000);
		conversationService.addMessage(conversationId, "후속 질문", "후속 응답");
		em.flush();
		em.clear();
		AIConversation updated = conversationRepository.findById(conversationId).orElseThrow();
		assertThat(updated.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS))
			.isAfterOrEqualTo(initialUpdatedAt.truncatedTo(ChronoUnit.SECONDS));

	}

}
