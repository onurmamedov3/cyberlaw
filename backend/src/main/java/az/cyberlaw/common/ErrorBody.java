package az.cyberlaw.common;

import java.util.Map;

/** The JSON body of every error response: {"code": "DOCUMENT_LIMIT", "message": "…", "params": {"max": 5}}. */
public record ErrorBody(String code, String message, Map<String, Object> params) {

	public static ErrorBody of(ErrorCode code) {
		return of(code, Map.of());
	}

	public static ErrorBody of(ErrorCode code, Map<String, Object> params) {
		return new ErrorBody(code.name(), code.message(params), params);
	}

}
