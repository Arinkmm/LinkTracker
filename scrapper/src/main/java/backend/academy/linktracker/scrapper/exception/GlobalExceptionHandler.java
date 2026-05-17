package backend.academy.linktracker.scrapper.exception;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
                .addKeyValue("error", e.getClass().getSimpleName())
                .addKeyValue("message", e.getMessage())
                .log("Rate limit exceeded");

        return createError(e, "429", e.getMessage());
    }

    @ExceptionHandler(ChatNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleChatNotFound(ChatNotFoundException e) {
        log.atWarn()
                .addKeyValue("error", e.getClass().getSimpleName())
                .addKeyValue("message", e.getMessage())
                .log("Chat not found");

        return createError(e, "404", e.getMessage());
    }

    @ExceptionHandler({ChatAlreadyExistsException.class, LinkAlreadyTrackedException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleConflict(RuntimeException e) {
        log.atWarn()
                .addKeyValue("error", e.getClass().getSimpleName())
                .addKeyValue("message", e.getMessage())
                .log("Conflict error");

        return createError(e, "409", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleGenericError(Exception e) {
        log.atError()
                .addKeyValue("error", e.getClass().getSimpleName())
                .addKeyValue("message", e.getMessage())
                .log("Controller error");

        return createError(e, "400", e.getMessage());
    }

    private ApiErrorResponse createError(Exception e, String code, String description) {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setCode(code);
        error.setDescription(description);
        error.setExceptionName(e.getClass().getSimpleName());
        error.setExceptionMessage(e.getMessage());
        error.setStacktrace(getStackTrace(e));
        return error;
    }

    private List<String> getStackTrace(Exception e) {
        return Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).toList();
    }
}
