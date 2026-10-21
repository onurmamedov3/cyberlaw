package az.cyberlaw.common;

import java.util.Map;

import org.springframework.http.HttpStatus;

/** An error with a stable code (translated by the frontend) and an English message that is safe to show. */
public class ApiException extends RuntimeException {

	private final ErrorCode code;

	private final Map<String, Object> params;

	public ApiException(ErrorCode code) {
		this(code, Map.of());
	}

	public ApiException(ErrorCode code, Map<String, Object> params) {
		super(code.message(params));
		this.code = code;
		this.params = Map.copyOf(params);
	}

	public ErrorCode getCode() {
		return code;
	}

	public HttpStatus getStatus() {
		return code.getStatus();
	}

	public Map<String, Object> getParams() {
		return params;
	}

}
