package az.cyberlaw.common;

import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Stable error codes sent to the frontend. The frontend shows its own translation ("error.CODE" in i18n.ts)
 * and falls back to the English message below. Placeholders such as {max} are filled from the params.
 * The AI errors (RATE_LIMIT, INVALID_KEY, …) have their own enum: {@link az.cyberlaw.ai.AiErrorCode}.
 */
public enum ErrorCode {

	// generic
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "The request was not valid."),
	VALIDATION(HttpStatus.BAD_REQUEST, "Please check the entered data."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Please log in again."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "You don't have permission to do this."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "Not found."),
	CONFLICT(HttpStatus.CONFLICT, "This conflicts with existing data. Reload the page and try again."),
	TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "The file is too large (max {maxMb} MB)."),
	INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on the server. Please try again."),

	// invalid form fields (@Valid): "INVALID_" + the field name, see forField()
	INVALID_EMAIL(HttpStatus.BAD_REQUEST, "Enter a valid email address."),
	INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "The password must be 8 to 100 characters long."),
	INVALID_DISPLAY_NAME(HttpStatus.BAD_REQUEST, "Enter your name (max 100 characters)."),
	INVALID_QUESTION(HttpStatus.BAD_REQUEST, "The question is empty or too long."),
	INVALID_CONVERSATION_ID(HttpStatus.BAD_REQUEST, "Invalid conversation. Please start a new chat."),
	INVALID_PER_MINUTE(HttpStatus.BAD_REQUEST, "Enter a whole number from 0 to 1000."),

	// accounts
	EMAIL_TAKEN(HttpStatus.CONFLICT, "An account with this email already exists."),
	WRONG_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Wrong email or password."),
	ACCOUNT_GONE(HttpStatus.UNAUTHORIZED, "This account no longer exists."),

	// chat
	TOO_MANY_QUESTIONS(HttpStatus.TOO_MANY_REQUESTS, "You are sending questions too quickly. Please wait {seconds} s."),

	// documents
	FILE_MISSING(HttpStatus.BAD_REQUEST, "Please choose a file."),
	FILE_TYPE(HttpStatus.BAD_REQUEST, "Unsupported file type. Use PDF, DOCX or TXT."),
	FILE_UNREADABLE(HttpStatus.BAD_REQUEST, "Could not read this file. It may be damaged or password-protected."),
	DOCUMENT_LIMIT(HttpStatus.BAD_REQUEST, "You can keep at most {max} private documents. Delete one first."),
	DUPLICATE_FILE(HttpStatus.CONFLICT, "This file has already been uploaded."),
	OFFICIAL_ADMIN_ONLY(HttpStatus.FORBIDDEN, "Only the admin can add documents to the Official Library."),
	DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Document not found."),
	DOCUMENT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "You cannot delete this document."),

	// stored on a document or report that failed in the background (never thrown, so the status is unused)
	NO_TEXT(HttpStatus.BAD_REQUEST, "No text could be extracted. Scanned PDFs (images) are not supported."),
	INDEXING_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR,
			"Indexing was interrupted by a server restart. Please upload the file again."),
	INDEXING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Indexing failed. Please upload the file again."),
	CHECK_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "Interrupted by a server restart. Please run the check again."),
	CHECK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "The check failed. Please try again."),

	// compliance
	POLICY_NOT_READY(HttpStatus.BAD_REQUEST, "The policy document is not indexed yet."),
	LAW_NOT_FOUND(HttpStatus.NOT_FOUND, "Law not found."),
	LAW_NO_CHECKLIST(HttpStatus.BAD_REQUEST, "This law has no compliance checklist yet."),
	CHECKLIST_NOT_ALLOWED(HttpStatus.BAD_REQUEST,
			"Only indexed documents of the Official Library can have a checklist."),
	CHECKLIST_RUNNING(HttpStatus.CONFLICT, "The checklist is already being generated."),
	REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "Report not found.");

	private final HttpStatus status;

	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	/** The English message with its {placeholders} filled in. */
	public String message(Map<String, ?> params) {
		String text = message;
		for (Map.Entry<String, ?> param : params.entrySet()) {
			text = text.replace("{" + param.getKey() + "}", String.valueOf(param.getValue()));
		}
		return text;
	}

	/** "displayName" → INVALID_DISPLAY_NAME, or VALIDATION for fields without their own code. */
	public static ErrorCode forField(String field) {
		String name = "INVALID_" + field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
		for (ErrorCode code : values()) {
			if (code.name().equals(name)) {
				return code;
			}
		}
		return VALIDATION;
	}

	/** For Spring's own errors, which only come with an HTTP status. */
	public static ErrorCode forStatus(HttpStatusCode status) {
		return switch (status.value()) {
			case 401 -> UNAUTHORIZED;
			case 403 -> FORBIDDEN;
			case 404 -> NOT_FOUND;
			case 409 -> CONFLICT;
			default -> status.is5xxServerError() ? INTERNAL : BAD_REQUEST;
		};
	}

}
