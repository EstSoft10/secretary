package est.secretary.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import est.secretary.domain.AIConversation;

public interface AIConversationRepository extends JpaRepository<AIConversation, Long> {
	List<AIConversation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
