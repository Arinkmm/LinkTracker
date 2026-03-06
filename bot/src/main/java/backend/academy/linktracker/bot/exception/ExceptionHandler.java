package backend.academy.linktracker.bot.exception;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionHandler {
    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleError(Exception e) {
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
