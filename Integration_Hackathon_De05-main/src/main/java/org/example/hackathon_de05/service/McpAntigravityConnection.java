package org.example.hackathon_de05.service;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class McpAntigravityConnection {
    private static final Logger logger = LoggerFactory.getLogger(McpAntigravityConnection.class);

    private final boolean enabled;
    private final String endpoint;
    private final String token;
    private volatile McpSyncClient client;

    public McpAntigravityConnection(
            @Value("${app.mcp.antigravity.enabled:false}") boolean enabled,
            @Value("${app.mcp.antigravity.endpoint:}") String endpoint,
            @Value("${app.mcp.antigravity.token:}") String token) {
        this.enabled = enabled;
        this.endpoint = endpoint;
        this.token = token;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connect() {
        if (!enabled) {
            logger.info("MCP Antigravity connection is disabled.");
            return;
        }
        if (endpoint.isBlank() || token.isBlank()) {
            logger.warn("MCP Antigravity connection skipped: endpoint or personal token is not configured.");
            return;
        }

        McpSyncClient candidate = null;
        try {
            HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(endpoint)
                    .connectTimeout(Duration.ofSeconds(4))
                    .httpRequestCustomizer((request, method, uri, body, context) ->
                            request.header("Authorization", "Bearer " + token))
                    .build();
            candidate = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(8))
                    .initializationTimeout(Duration.ofSeconds(8))
                    .build();
            candidate.initialize();
            if (!candidate.isInitialized()) {
                throw new IllegalStateException("MCP initialization did not complete.");
            }
            client = candidate;
            logger.info("MCP Antigravity connection initialized.");
        } catch (RuntimeException exception) {
            if (candidate != null) {
                candidate.close();
            }
            logger.warn("MCP Antigravity is unavailable; application continues without MCP. exceptionType={}",
                    exception.getClass().getSimpleName());
        }
    }

    public boolean isConnected() {
        McpSyncClient activeClient = client;
        return activeClient != null && activeClient.isInitialized();
    }

    @PreDestroy
    public void close() {
        McpSyncClient activeClient = client;
        client = null;
        if (activeClient != null) {
            activeClient.close();
        }
    }
}
