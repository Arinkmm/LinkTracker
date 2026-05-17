package backend.academy.linktracker.bot.exception;

import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Slf4j
public final class ApiExceptionMapper {
    private ApiExceptionMapper() {}

    public static ApiException fromHttpResponse(
            ObjectMapper objectMapper,
            HttpStatusCode statusCode,
            String rawBody,
            String fallbackDescription,
            String upstreamName) {
        try {
            ApiErrorResponse error = objectMapper.readValue(rawBody, ApiErrorResponse.class);
            if (error.getCode() == null) {
                error.setCode(String.valueOf(statusCode.value()));
            }
            return new ApiException(error);
        } catch (Exception e) {
            log.atWarn()
                    .addKeyValue("upstream", upstreamName)
                    .addKeyValue("status", statusCode.value())
                    .addKeyValue("reason", e.getMessage())
                    .log("Failed to parse HTTP error response, using fallback API error");
            return new ApiException(fallbackHttpError(statusCode, fallbackDescription, rawBody, e));
        }
    }

    public static ApiException fromGrpcException(StatusRuntimeException exception, String upstreamName) {
        Status status = exception.getStatus();
        HttpStatus httpStatus = toHttpStatus(status.getCode());
        String description = status.getDescription();

        log.atWarn()
                .addKeyValue("upstream", upstreamName)
                .addKeyValue("grpcStatus", status.getCode())
                .addKeyValue("mappedHttpStatus", httpStatus.value())
                .addKeyValue("description", description)
                .addKeyValue("cause", rootCauseMessage(exception))
                .log("gRPC call failed");

        ApiErrorResponse error = new ApiErrorResponse();
        error.setCode(String.valueOf(httpStatus.value()));
        error.setDescription(description != null ? description : httpStatus.getReasonPhrase());
        error.setExceptionName(exception.getClass().getSimpleName());
        error.setExceptionMessage(exception.getMessage());
        error.setStacktrace(List.of());

        return new ApiException(error);
    }

    public static ApiException circuitBreakerOpen(CallNotPermittedException exception, String upstreamName) {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setCode(String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()));
        error.setDescription(upstreamName + " is temporarily unavailable");
        error.setExceptionName(exception.getClass().getSimpleName());
        error.setExceptionMessage(exception.getMessage());
        error.setStacktrace(List.of());

        return new ApiException(error);
    }

    private static ApiErrorResponse fallbackHttpError(
            HttpStatusCode statusCode, String fallbackDescription, String rawBody, Exception cause) {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setCode(String.valueOf(statusCode.value()));
        error.setDescription(fallbackDescription);
        error.setExceptionName(cause.getClass().getSimpleName());
        error.setExceptionMessage(rawBody);
        error.setStacktrace(List.of());
        return error;
    }

    private static HttpStatus toHttpStatus(Status.Code code) {
        return switch (code) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            case RESOURCE_EXHAUSTED -> HttpStatus.TOO_MANY_REQUESTS;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case INTERNAL, UNKNOWN -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private static String rootCauseMessage(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
