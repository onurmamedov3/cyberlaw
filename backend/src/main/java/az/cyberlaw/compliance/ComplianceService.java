package az.cyberlaw.compliance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import az.cyberlaw.ai.AiErrorCode;
import az.cyberlaw.ai.AiException;
import az.cyberlaw.common.ApiException;
import az.cyberlaw.common.ErrorCode;
import az.cyberlaw.compliance.ComplianceDtos.LawDto;
import az.cyberlaw.compliance.ComplianceDtos.ReportDetailDto;
import az.cyberlaw.compliance.ComplianceDtos.ReportDto;
import az.cyberlaw.compliance.ComplianceDtos.RequirementDto;
import az.cyberlaw.compliance.ComplianceDtos.ResultDto;
import az.cyberlaw.config.CyberLawProperties;
import az.cyberlaw.document.DocumentDto;
import az.cyberlaw.document.DocumentScope;
import az.cyberlaw.document.DocumentService;
import az.cyberlaw.document.DocumentStatus;
import az.cyberlaw.document.LibraryDocument;
import az.cyberlaw.document.LibraryDocumentRepository;
import az.cyberlaw.document.RequirementsStatus;
import az.cyberlaw.security.CurrentUser;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ComplianceService {

	private final LibraryDocumentRepository documents;

	private final DocumentService documentService;

	private final RequirementRepository requirements;

	private final ComplianceReportRepository reports;

	private final ComplianceResultRepository results;

	private final RequirementExtractionService extraction;

	private final ComplianceRunner runner;

	private final CyberLawProperties props;

	public ComplianceService(LibraryDocumentRepository documents, DocumentService documentService,
			RequirementRepository requirements, ComplianceReportRepository reports,
			ComplianceResultRepository results, RequirementExtractionService extraction, ComplianceRunner runner,
			CyberLawProperties props) {
		this.documents = documents;
		this.documentService = documentService;
		this.requirements = requirements;
		this.reports = reports;
		this.results = results;
		this.extraction = extraction;
		this.runner = runner;
		this.props = props;
	}

	/** Laws that have a checklist and can be used for a compliance check. */
	public List<LawDto> laws() {
		return documents.findByScopeOrderByTitle(DocumentScope.OFFICIAL)
			.stream()
			.filter(d -> d.getRequirementsStatus() == RequirementsStatus.READY)
			.map(d -> new LawDto(d.getId().toString(), d.getTitle(), d.getLanguage(),
					requirements.countByDocumentId(d.getId())))
			.toList();
	}

	public List<RequirementDto> requirements(CurrentUser user, UUID lawId) {
		documentService.getVisible(user, lawId);
		return requirements.findByDocumentIdOrderByPosition(lawId).stream().map(RequirementDto::of).toList();
	}

	/** Admin action: build the checklist for an official law (runs in the background). */
	public DocumentDto startExtraction(CurrentUser admin, UUID lawId) {
		requireAi();
		LibraryDocument law = documents.findById(lawId)
			.orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
		if (law.getScope() != DocumentScope.OFFICIAL || law.getStatus() != DocumentStatus.READY) {
			throw new ApiException(ErrorCode.CHECKLIST_NOT_ALLOWED);
		}
		if (law.getRequirementsStatus() == RequirementsStatus.EXTRACTING) {
			throw new ApiException(ErrorCode.CHECKLIST_RUNNING);
		}
		law.setRequirementsStatus(RequirementsStatus.EXTRACTING);
		documents.save(law);
		extraction.extract(lawId);
		return DocumentDto.of(law, admin.id());
	}

	public ReportDto start(CurrentUser user, UUID policyId, UUID lawId, String language) {
		requireAi();
		LibraryDocument policy = documentService.getVisible(user, policyId);
		if (policy.getStatus() != DocumentStatus.READY) {
			throw new ApiException(ErrorCode.POLICY_NOT_READY);
		}
		LibraryDocument law = documents.findById(lawId).orElseThrow(() -> new ApiException(ErrorCode.LAW_NOT_FOUND));
		if (law.getScope() != DocumentScope.OFFICIAL || law.getRequirementsStatus() != RequirementsStatus.READY) {
			throw new ApiException(ErrorCode.LAW_NO_CHECKLIST);
		}
		int total = (int) requirements.countByDocumentId(lawId);
		String lang = "az".equals(language) ? "az" : "en";
		ComplianceReport report = reports.save(new ComplianceReport(user.id(), policyId, lawId, lang, total));
		runner.run(report.getId());
		return ReportDto.of(report, policy, law);
	}

	public List<ReportDto> list(CurrentUser user) {
		List<ComplianceReport> own = reports.findByOwnerIdOrderByCreatedAtDesc(user.id());
		Map<UUID, LibraryDocument> docs = byId(own.stream()
			.flatMap(r -> Stream.of(r.getPolicyDocumentId(), r.getLawDocumentId()))
			.distinct()
			.toList());
		return own.stream()
			.map(r -> ReportDto.of(r, docs.get(r.getPolicyDocumentId()), docs.get(r.getLawDocumentId())))
			.toList();
	}

	public ReportDetailDto get(CurrentUser user, UUID id) {
		ComplianceReport report = owned(user, id);
		Map<UUID, LibraryDocument> docs = byId(List.of(report.getPolicyDocumentId(), report.getLawDocumentId()));
		List<ResultDto> rows = results.findByReportIdOrderByPosition(id).stream().map(ResultDto::of).toList();
		return new ReportDetailDto(ReportDto.of(report, docs.get(report.getPolicyDocumentId()),
				docs.get(report.getLawDocumentId())), rows);
	}

	public void delete(CurrentUser user, UUID id) {
		reports.delete(owned(user, id));
	}

	/** Reports that were running when the server stopped can never finish. */
	@EventListener(ApplicationReadyEvent.class)
	public void failInterruptedReports() {
		for (ComplianceReport report : reports.findByStatus(ComplianceReport.Status.RUNNING)) {
			report.fail(ErrorCode.CHECK_INTERRUPTED.name(), ErrorCode.CHECK_INTERRUPTED.message());
			reports.save(report);
		}
	}

	private ComplianceReport owned(CurrentUser user, UUID id) {
		return reports.findById(id)
			.filter(r -> r.getOwnerId().equals(user.id()))
			.orElseThrow(() -> new ApiException(ErrorCode.REPORT_NOT_FOUND));
	}

	private Map<UUID, LibraryDocument> byId(List<UUID> ids) {
		Map<UUID, LibraryDocument> docs = new HashMap<>();
		documents.findAllById(ids).forEach(d -> docs.put(d.getId(), d));
		return docs;
	}

	private void requireAi() {
		if (!props.aiConfigured()) {
			throw new AiException(AiErrorCode.NOT_CONFIGURED, AiException.describe(AiErrorCode.NOT_CONFIGURED));
		}
	}

}
