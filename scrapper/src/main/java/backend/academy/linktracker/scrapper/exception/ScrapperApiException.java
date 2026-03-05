package backend.academy.linktracker.scrapper.exception;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import lombok.Getter;
import java.util.List;

@Getter
public class ScrapperApiException extends RuntimeException {
    private final ApiErrorResponse apiError;

    public ScrapperApiException(ApiErrorResponse apiError) {
        super(apiError.description());
        this.apiError = apiError;
    }
}
