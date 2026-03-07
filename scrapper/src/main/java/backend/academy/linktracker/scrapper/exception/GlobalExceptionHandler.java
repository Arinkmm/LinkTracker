package backend.academy.linktracker.scrapper.exception;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ChatNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleChatNotFound(ChatNotFoundException e) {
        log.atWarn()
                .addKeyValue("error", e.getClass().getSimpleName())
                .addKeyValue("message", e.getMessage())
                .log("Chat not found");

        return new ApiErrorResponse(
                e.getMessage(), "404", e.getClass().getSimpleName(), e.getMessage(), getStackTrace(e));
    }

    @ExceptionHandler({ChatAlreadyExistsException.class, LinkAlreadyTrackedException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleConflict(RuntimeException e) {
        log.atWarn()
                .addKeyValue("error", e.getClass().getSimpleName())
                .addKeyValue("message", e.getMessage())
                .log("Conflict error");

        return new ApiErrorResponse(
                e.getMessage(), "409", e.getClass().getSimpleName(), e.getMessage(), getStackTrace(e));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleGenericError(Exception e) {
        log.atError()
                .addKeyValue("error", e.getClass().getSimpleName())
                .addKeyValue("message", e.getMessage())
                .log("Controller error");

        return new ApiErrorResponse(
                e.getMessage(), "400", e.getClass().getSimpleName(), e.getMessage(), getStackTrace(e));
    }

    private List<String> getStackTrace(Exception e) {
        return Arrays.stream(e.getStackTrace())
                .map(StackTraceElement::toString)
                .limit(10)
                .collect(Collectors.toList());
    }
}
