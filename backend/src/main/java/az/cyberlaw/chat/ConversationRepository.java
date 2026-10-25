package az.cyberlaw.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

	List<Conversation> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId, Pageable page);

	/** Messages and sources go with them (ON DELETE CASCADE). */
	@Modifying
	@Query("delete from Conversation c where c.ownerId = :ownerId")
	int deleteByOwner(UUID ownerId);

	@Modifying
	@Query("delete from Conversation c where c.updatedAt < :before")
	int deleteNotUpdatedSince(Instant before);

}
