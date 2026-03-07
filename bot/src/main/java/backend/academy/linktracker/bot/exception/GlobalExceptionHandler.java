package backend.academy.linktracker.bot.exception;

import backend.academy.linktracker.bot.dto.ApiErrorResponse;
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
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleError(Exception e) {
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
