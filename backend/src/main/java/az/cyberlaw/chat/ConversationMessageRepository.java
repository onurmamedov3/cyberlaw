package az.cyberlaw.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

	List<ConversationMessage> findByConversationIdOrderByPosition(UUID conversationId);

	/** The newest messages first. */
	List<ConversationMessage> findByConversationIdOrderByPositionDesc(UUID conversationId, Pageable page);

	long countByConversationId(UUID conversationId);

}
