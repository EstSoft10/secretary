package est.secretary.service;

import java.util.List;

import org.springframework.stereotype.Service;

import est.secretary.domain.AIConversation;
import est.secretary.domain.AIMessage;
import est.secretary.repository.AIConversationRepository;
import est.secretary.repository.AIMessageRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AIConversationService {

	private final AIConversationRepository conversationRepo;
	private final AIMessageRepository messageRepo;

	public AIConversation createConversation(Long userId, String userMessage, String aiResponse) {
		AIConversation conversation = AIConversation.builder()
			.userId(userId)
			.build();

		AIMessage userMsg = AIMessage.builder()
			.conversation(conversation)
			.sender(AIMessage.Sender.USER)
			.message(userMessage)
			.build();

		AIMessage aiMsg = AIMessage.builder()
			.conversation(conversation)
			.sender(AIMessage.Sender.AI)
			.message(aiResponse)
			.build();

		conversation.getMessages().add(userMsg);
		conversation.getMessages().add(aiMsg);

		return conversationRepo.save(conversation);
	}

	public void addMessage(Long conversationId, String userMessage, String aiResponse) {
		AIConversation conversation = conversationRepo.findById(conversationId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대화 ID"));

		AIMessage userMsg = AIMessage.builder()
			.conversation(conversation)
			.sender(AIMessage.Sender.USER)
			.message(userMessage)
			.build();

		AIMessage aiMsg = AIMessage.builder()
			.conversation(conversation)
			.sender(AIMessage.Sender.AI)
			.message(aiResponse)
			.build();

		messageRepo.save(userMsg);
		messageRepo.save(aiMsg);

		conversationRepo.save(conversation);
	}

	public List<AIConversation> getConversations(Long userId) {
		return conversationRepo.findByUserIdOrderByUpdatedAtDesc(userId);
	}

	public List<AIMessage> getMessages(Long conversationId) {
		return messageRepo.findByConversationIdOrderByCreatedAt(conversationId);
	}

	public void deleteConversation(Long conversationId) {
		conversationRepo.deleteById(conversationId);
	}
}
