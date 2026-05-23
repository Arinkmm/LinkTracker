package backend.academy.linktracker.bot.exception;

import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.bot.properties.RetryProperties;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScrapperClientExceptionFactory {
    private final MessagesProperties messagesProperties;
    private final RetryProperties retryProperties;

    public ApiException fromHttpStatus(HttpStatusCode statusCode, String responseBody) {
        return createException(statusCode.value(), messagesProperties.getInvalidResponse(), responseBody);
    }

    public ApiException fromGrpcStatus(StatusRuntimeException exception) {
        Status status = exception.getStatus();
        HttpStatus httpStatus = toHttpStatus(status.getCode());
        String description = status.getDescription() != null ? status.getDescription() : httpStatus.getReasonPhrase();

        return createException(httpStatus.value(), description, exception.getMessage());
    }

    private ApiException createException(int statusCode, String description, String message) {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setCode(String.valueOf(statusCode));
        error.setDescription(description);
        error.setExceptionMessage(message);
        error.setStacktrace(List.of());

        if (retryProperties.getRetryableStatusCodes().contains(statusCode)) {
            error.setExceptionName(RetryableApiException.class.getSimpleName());
            return new RetryableApiException(error);
        }

        error.setExceptionName(ApiException.class.getSimpleName());
        return new ApiException(error);
    }

    private HttpStatus toHttpStatus(Status.Code code) {
        HttpStatus status =
                switch (code) {
                    case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
                    case NOT_FOUND -> HttpStatus.NOT_FOUND;
                    case ALREADY_EXISTS -> HttpStatus.CONFLICT;
                    case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
                    case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
                    case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
                    case RESOURCE_EXHAUSTED -> HttpStatus.TOO_MANY_REQUESTS;
                    case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
                    case INTERNAL, UNKNOWN -> HttpStatus.INTERNAL_SERVER_ERROR;
                    default -> HttpStatus.INTERNAL_SERVER_ERROR;
                };

        if (status == HttpStatus.INTERNAL_SERVER_ERROR && code != Status.Code.INTERNAL && code != Status.Code.UNKNOWN) {
            log.atWarn().addKeyValue("grpcStatus", code).log("Unexpected gRPC status mapped to HTTP 500");
        }
        return status;
    }
}
