package backend.academy.linktracker.ai.dto;

import java.util.List;

public record ChatCompletionResponse(List<ChatChoice> choices) {}
