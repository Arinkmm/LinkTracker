package backend.academy.linktracker.bot.exception;

import backend.academy.linktracker.bot.dto.ApiErrorResponse;
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

        ApiErrorResponse error = new ApiErrorResponse();
        error.setDescription(e.getMessage());
        error.setCode(String.valueOf(HttpStatus.TOO_MANY_REQUESTS.value()));
        error.setExceptionName(e.getClass().getSimpleName());
        error.setExceptionMessage(e.getMessage());
        error.setStacktrace(getStackTrace(e));

        return error;
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

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleError(Exception e) {
        log.atError()
                .setCause(e)
                .addKeyValue("error", e.getClass().getSimpleName())
                .addKeyValue("message", e.getMessage())
                .log("Controller error");

        ApiErrorResponse error = new ApiErrorResponse();
        error.setDescription(e.getMessage());
        error.setCode(String.valueOf(HttpStatus.BAD_REQUEST.value()));
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
