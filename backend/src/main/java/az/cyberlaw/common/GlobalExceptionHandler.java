package az.cyberlaw.common;

import java.util.Map;
import java.util.stream.Collectors;

import az.cyberlaw.ai.AiErrorCode;
import az.cyberlaw.ai.AiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<Map<String, String>> handleApi(ApiException ex) {
		return error(ex.getStatus(), "ERROR", ex.getMessage());
	}

	@ExceptionHandler(AiException.class)
	ResponseEntity<Map<String, String>> handleAi(AiException ex) {
		HttpStatus status = ex.getCode() == AiErrorCode.RATE_LIMIT ? HttpStatus.TOO_MANY_REQUESTS
				: HttpStatus.SERVICE_UNAVAILABLE;
		return error(status, ex.getCode().name(), ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(e -> e.getField() + ": " + e.getDefaultMessage())
			.collect(Collectors.joining("; "));
		return error(HttpStatus.BAD_REQUEST, "VALIDATION", message);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<Map<String, String>> handleTooLarge(MaxUploadSizeExceededException ex) {
		return error(HttpStatus.PAYLOAD_TOO_LARGE, "TOO_LARGE", "File is too large (max 10 MB).");
	}

	private static ResponseEntity<Map<String, String>> error(HttpStatus status, String code, String message) {
		if (status.is5xxServerError()) {
			log.warn("{}: {}", code, message);
		}
		return ResponseEntity.status(status).body(Map.of("code", code, "message", message));
	}

}
