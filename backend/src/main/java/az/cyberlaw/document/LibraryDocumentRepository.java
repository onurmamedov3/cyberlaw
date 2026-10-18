package az.cyberlaw.document;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LibraryDocumentRepository extends JpaRepository<LibraryDocument, UUID> {

	@Query("""
			select d from LibraryDocument d
			where d.scope = az.cyberlaw.document.DocumentScope.OFFICIAL or d.ownerId = :userId
			order by d.scope, d.createdAt desc""")
	List<LibraryDocument> findVisibleTo(UUID userId);

	List<LibraryDocument> findByScopeOrderByTitle(DocumentScope scope);

	long countByOwnerIdAndScope(UUID ownerId, DocumentScope scope);

	boolean existsBySha256AndScope(String sha256, DocumentScope scope);

	boolean existsBySha256AndOwnerId(String sha256, UUID ownerId);

	List<LibraryDocument> findByScopeAndExpiresAtBefore(DocumentScope scope, Instant time);

	List<LibraryDocument> findByStatusIn(Collection<DocumentStatus> statuses);

	List<LibraryDocument> findByScopeAndRequirementsStatus(DocumentScope scope, RequirementsStatus status);

}
