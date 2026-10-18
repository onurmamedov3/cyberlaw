package az.cyberlaw.compliance;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceReportRepository extends JpaRepository<ComplianceReport, UUID> {

	List<ComplianceReport> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	List<ComplianceReport> findByStatus(ComplianceReport.Status status);

}
