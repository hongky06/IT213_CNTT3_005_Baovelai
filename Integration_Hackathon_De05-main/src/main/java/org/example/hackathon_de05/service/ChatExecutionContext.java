package org.example.hackathon_de05.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ChatExecutionContext {
    private final ThreadLocal<Execution> current = new ThreadLocal<>();

    public Scope open(String conversationId) {
        Execution execution = new Execution(conversationId);
        current.set(execution);
        return new Scope(execution, current);
    }

    public void recordTool(String toolName) {
        Execution execution = current.get();
        if (execution != null) {
            execution.toolsUsed.add(toolName);
        }
    }

    public void recordSource(RAGService.SourceReference source) {
        Execution execution = current.get();
        if (execution != null) {
            execution.sources.putIfAbsent(source.id(), source);
        }
    }

    private static final class Execution {
        private final String conversationId;
        private final Set<String> toolsUsed = new LinkedHashSet<>();
        private final Map<String, RAGService.SourceReference> sources = new LinkedHashMap<>();

        private Execution(String conversationId) {
            this.conversationId = conversationId;
        }
    }

    public static final class Scope implements AutoCloseable {
        private final Execution execution;
        private final ThreadLocal<Execution> slot;
        private boolean closed;

        private Scope(Execution execution, ThreadLocal<Execution> slot) {
            this.execution = execution;
            this.slot = slot;
        }

        public String conversationId() {
            return execution.conversationId;
        }

        public List<String> toolsUsed() {
            return new ArrayList<>(execution.toolsUsed);
        }

        public boolean usedTool(String name) {
            return execution.toolsUsed.contains(name);
        }

        public List<RAGService.SourceReference> sources() {
            return new ArrayList<>(execution.sources.values());
        }

        @Override
        public void close() {
            if (!closed) {
                slot.remove();
                closed = true;
            }
        }
    }
}
