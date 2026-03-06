package backend.academy.linktracker.scrapper.exception;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionHandler {
    @org.springframework.web.bind.annotation.ExceptionHandler(ChatNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleChatNotFound(ChatNotFoundException e) {
        return new ApiErrorResponse(
            e.getMessage(), "404",
            e.getClass().getSimpleName(),
            e.getMessage(),
            getStackTrace(e)
        );
    }

    @org.springframework.web.bind.annotation.ExceptionHandler({ChatAlreadyExistsException.class, LinkAlreadyTrackedException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleConflict(RuntimeException e) {
        return new ApiErrorResponse(
            e.getMessage(), "409",
            e.getClass().getSimpleName(),
            e.getMessage(),
            getStackTrace(e)
        );
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleGenericError(Exception e) {
        return new ApiErrorResponse(
            e.getMessage(), "400",
            e.getClass().getSimpleName(),
            e.getMessage(),
            getStackTrace(e)
        );
    }

    private List<String> getStackTrace(Exception e) {
        return Arrays.stream(e.getStackTrace())
            .map(StackTraceElement::toString)
            .limit(10)
            .collect(Collectors.toList());
    }
}
