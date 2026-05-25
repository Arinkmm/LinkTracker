package backend.academy.linktracker.ai.exception;

public class AiSummarizationException extends RuntimeException {
    public AiSummarizationException(String message) {
        super(message);
    }

    public AiSummarizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
