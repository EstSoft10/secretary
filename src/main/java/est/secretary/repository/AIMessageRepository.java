package est.secretary.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import est.secretary.domain.AIMessage;

public interface AIMessageRepository extends JpaRepository<AIMessage, Long> {
	List<AIMessage> findByConversationIdOrderByCreatedAt(Long conversationId);

	void deleteByConversationId(Long conversationId);
}
