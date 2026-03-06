package backend.academy.linktracker.bot.exception;

import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final ApiErrorResponse apiError;

    public ApiException(ApiErrorResponse apiError) {
        super(apiError.description());
        this.apiError = apiError;
    }
}
