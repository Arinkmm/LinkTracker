package backend.academy.linktracker.scrapper.exception;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiErrorResponse handleRateLimitExceeded(RateLimitExceededException e) {
        log.atWarn()
                .setCause(e)
                .addKeyValue("error", e.getClass().getSimpleName())
                .addKeyValue("message", e.getMessage())
                .log("Rate limit exceeded");

        return createError(e, HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException e) {
        log.atWarn()
                .setCause(e)
                .addKeyValue("error", e.getClass().getSimpleName())
                .addKeyValue("code", e.getApiError().getCode())
                .addKeyValue("message", e.getMessage())
                .log("API error");

        HttpStatus status = resolveStatus(e.getApiError().getCode());
        return ResponseEntity.status(status).body(e.getApiError());
    }

    @ExceptionHandler(ChatNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleChatNotFound(ChatNotFoundException e) {
        log.atWarn()
                .setCause(e)
                .addKeyValue("error", e.getClass().getSimpleName())
                .addKeyValue("message", e.getMessage())
                .log("Chat not found");

        return createError(e, HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({ChatAlreadyExistsException.class, LinkAlreadyTrackedException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleConflict(RuntimeException e) {
        log.atWarn()
                .setCause(e)
                .addKeyValue("error", e.getClass().getSimpleName())
                .addKeyValue("message", e.getMessage())
                .log("Conflict error");

        return createError(e, HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleGenericError(Exception e) {
        log.atError()
                .setCause(e)
                .addKeyValue("error", e.getClass().getSimpleName())
                .addKeyValue("message", e.getMessage())
                .log("Controller error");

        return createError(e, HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ApiErrorResponse createError(Exception e, HttpStatus status, String description) {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setCode(String.valueOf(status.value()));
        error.setDescription(description);
        error.setExceptionName(e.getClass().getSimpleName());
        error.setExceptionMessage(e.getMessage());
        error.setStacktrace(getStackTrace(e));
        return error;
    }

    private List<String> getStackTrace(Exception e) {
        return Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).toList();
    }

    private HttpStatus resolveStatus(String code) {
        try {
            HttpStatus status = HttpStatus.resolve(Integer.parseInt(code));
            if (status != null) {
                return status;
            }
        } catch (NumberFormatException e) {
            log.atWarn().setCause(e).addKeyValue("code", code).log("Invalid API error status code");
        }
        log.atWarn().addKeyValue("code", code).log("Unknown API error status code");
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
