package backend.academy.linktracker.ai.dto;

import java.util.List;

public record ChatCompletionRequest(String model, List<ChatMessage> messages, boolean stream) {}
