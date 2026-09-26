package org.example.hackathon_de05.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ChatService {
    private static final String EXAM_CODE = "DE-005";

    private final ChatClient chatClient;
    private final ChatExecutionContext executionContext;
    private final Tracer tracer;

    public ChatService(ChatClient chatClient, ChatExecutionContext executionContext, Tracer tracer) {
        this.chatClient = chatClient;
        this.executionContext = executionContext;
        this.tracer = tracer;
    }

    public ChatResult ask(String conversationId, String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Câu hỏi không được để trống.");
        }
        String id = conversationId == null || conversationId.isBlank()
                ? UUID.randomUUID().toString() : conversationId.trim();

        Span span = tracer.spanBuilder("POST /api/assistant/ask")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        span.setAttribute("conversationId", id);
        span.setAttribute("examCode", EXAM_CODE);
        long startedAt = System.nanoTime();

        try (Scope ignored = span.makeCurrent();
             ChatExecutionContext.Scope execution = executionContext.open(id)) {
            String answer = chatClient.prompt()
                    .user(question.trim())
                    .options(ChatOptions.builder().temperature(0.2))
                    .advisors(advisor -> advisor.param("conversationId", id))
                    .call()
                    .content();
            List<String> toolsUsed = execution.toolsUsed();
            List<RAGService.SourceReference> sources = execution.sources();
            span.setAttribute("toolsUsed", String.join(",", toolsUsed));
            span.setAttribute("sources", sources.stream()
                    .map(source -> source.id() + "|" + source.section() + "|" + source.source())
                    .reduce((left, right) -> left + "," + right)
                    .orElse(""));
            span.setAttribute("durationMs", (System.nanoTime() - startedAt) / 1_000_000);
            if (execution.usedTool("searchInternalPolicies") && sources.isEmpty()) {
                answer = RAGService.FALLBACK_ANSWER;
            }
            return new ChatResult(Objects.requireNonNull(answer, "Chat model returned no answer."),
                    id, sources, toolsUsed);
        } catch (RuntimeException exception) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR);
            throw exception;
        } finally {
            span.end();
        }
    }

    public record ChatResult(String answer, String conversationId,
                             List<RAGService.SourceReference> sources, List<String> toolsUsed) {
    }
}
