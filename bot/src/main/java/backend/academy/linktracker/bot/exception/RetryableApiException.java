package backend.academy.linktracker.bot.exception;

import backend.academy.linktracker.bot.dto.ApiErrorResponse;

public class RetryableApiException extends ApiException {
    public RetryableApiException(ApiErrorResponse apiError) {
        super(apiError);
    }
}
