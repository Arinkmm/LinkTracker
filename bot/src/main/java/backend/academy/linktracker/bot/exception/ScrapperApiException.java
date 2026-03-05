package backend.academy.linktracker.bot.exception;

import backend.academy.linktracker.bot.client.dto.ApiErrorResponse;
import lombok.Getter;

@Getter
public class ScrapperApiException extends RuntimeException {
    private final ApiErrorResponse apiError;

    public ScrapperApiException(ApiErrorResponse apiError) {
        super(apiError.description());
        this.apiError = apiError;
    }
}
