package az.cyberlaw.compliance;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceResultRepository extends JpaRepository<ComplianceResult, UUID> {

	List<ComplianceResult> findByReportIdOrderByPosition(UUID reportId);

}
