package az.cyberlaw.common;

import java.util.Map;

import az.cyberlaw.ai.AiErrorCode;
import az.cyberlaw.ai.AiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into the same JSON body ({@link ErrorBody}). The base class handles Spring's own errors
 * (malformed JSON, an id that is not a UUID, an unknown URL, …) and skips responses that are already being
 * streamed; {@link #createResponseEntity} gives those the same body as ours.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private final long maxUploadMb;

	public GlobalExceptionHandler(@Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize) {
		this.maxUploadMb = maxFileSize.toMegabytes();
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<Object> handleApi(ApiException ex) {
		return error(ex.getStatus(), new ErrorBody(ex.getCode().name(), ex.getMessage(), ex.getParams()));
	}

	@ExceptionHandler(AiException.class)
	ResponseEntity<Object> handleAi(AiException ex) {
		HttpStatus status = ex.getCode() == AiErrorCode.RATE_LIMIT ? HttpStatus.TOO_MANY_REQUESTS
				: HttpStatus.SERVICE_UNAVAILABLE;
		return error(status, new ErrorBody(ex.getCode().name(), ex.getMessage(), Map.of()));
	}

	/** A unique constraint was hit, e.g. two changes of the same data at the same moment. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<Object> handleConflict(DataIntegrityViolationException ex) {
		log.warn("Data conflict: {}", ex.getMostSpecificCause().getMessage());
		return error(HttpStatus.CONFLICT, ErrorBody.of(ErrorCode.CONFLICT));
	}

	/** Anything unexpected: the details go to the log, the user gets a safe message. */
	@ExceptionHandler(Exception.class)
	ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest request, HttpServletResponse response) {
		if (response.isCommitted()) {
			// e.g. the browser closed a streamed answer: nothing can be sent any more
			log.debug("Error after the response was sent on {}: {}", request.getRequestURI(), ex.toString());
			return null;
		}
		log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return json(HttpStatus.INTERNAL_SERVER_ERROR, new HttpHeaders(), ErrorBody.of(ErrorCode.INTERNAL));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		FieldError field = ex.getBindingResult().getFieldError();
		ErrorCode code = field != null ? ErrorCode.forField(field.getField()) : ErrorCode.VALIDATION;
		return handleExceptionInternal(ex, ErrorBody.of(code), headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ErrorBody body = ErrorBody.of(ErrorCode.TOO_LARGE, Map.of("maxMb", maxUploadMb));
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
			WebRequest request) {
		Object error = body instanceof ErrorBody ? body : ErrorBody.of(ErrorCode.forStatus(statusCode));
		return json(statusCode, headers, error);
	}

	private static ResponseEntity<Object> error(HttpStatusCode status, ErrorBody body) {
		if (status.is5xxServerError()) {
			log.warn("{}: {}", body.code(), body.message());
		}
		return json(status, new HttpHeaders(), body);
	}

	/**
	 * Errors are always JSON, even for a request that accepts only "text/event-stream" (the chat stream):
	 * a preset content type skips content negotiation, which would otherwise find no way to write the body.
	 */
	private static ResponseEntity<Object> json(HttpStatusCode status, HttpHeaders headers, Object body) {
		return ResponseEntity.status(status).headers(headers).contentType(MediaType.APPLICATION_JSON).body(body);
	}

}
