package az.cyberlaw.compliance;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface RequirementRepository extends JpaRepository<Requirement, UUID> {

	List<Requirement> findByDocumentIdOrderByPosition(UUID documentId);

	long countByDocumentId(UUID documentId);

	@Transactional
	void deleteByDocumentId(UUID documentId);

}
