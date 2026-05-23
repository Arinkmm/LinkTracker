package backend.academy.linktracker.scrapper.exception;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.properties.ErrorProperties;
import backend.academy.linktracker.scrapper.properties.RetryProperties;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExternalApiExceptionFactory {
    private final ErrorProperties errorProperties;
    private final RetryProperties retryProperties;

    public ApiException fromHttpStatus(HttpStatusCode statusCode, URI uri) {
        int status = statusCode.value();

        ApiErrorResponse error = new ApiErrorResponse();
        error.setCode(String.valueOf(status));
        error.setDescription(errorProperties.getInvalidResponse());
        error.setExceptionMessage("External API returned HTTP " + status + " for " + uri);
        error.setStacktrace(List.of());

        if (retryProperties.getRetryableStatusCodes().contains(status)) {
            error.setExceptionName(RetryableApiException.class.getSimpleName());
            return new RetryableApiException(error);
        }

        error.setExceptionName(ApiException.class.getSimpleName());
        return new ApiException(error);
    }
}
