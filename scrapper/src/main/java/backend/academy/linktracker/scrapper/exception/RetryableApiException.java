package backend.academy.linktracker.scrapper.exception;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;

public class RetryableApiException extends ApiException {
    public RetryableApiException(ApiErrorResponse apiError) {
        super(apiError);
    }
}
