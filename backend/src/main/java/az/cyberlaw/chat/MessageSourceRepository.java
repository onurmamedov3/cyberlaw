package az.cyberlaw.chat;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MessageSourceRepository extends JpaRepository<MessageSource, UUID> {

	List<MessageSource> findByMessageIdInOrderByPosition(Collection<UUID> messageIds);

	@Modifying
	@Query("update MessageSource s set s.text = null where s.documentId = :documentId")
	int forgetText(UUID documentId);

}
