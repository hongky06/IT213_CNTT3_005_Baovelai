package org.example.hackathon_de05.controller;

import lombok.RequiredArgsConstructor;
import org.example.hackathon_de05.service.ChatService;
import org.example.hackathon_de05.service.RAGService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final RAGService ragService;

    @PostMapping("/admin/ingest-store-info")
    public Map<String, Object> ingestStoreInfo() throws IOException {
        int chunks = ragService.ingestPolicyCorpus();
        return Map.of("status", "success", "ingestedChunks", chunks,
                "source", "tai_lieu_noi_bo.md");
    }

    @PostMapping("/chat")
    public ChatService.ChatResult chat(@Valid @RequestBody LegacyChatRequest payload) {
        return chatService.ask(payload.conversationId(), payload.question());
    }

    @GetMapping("/chat")
    public ChatService.ChatResult chatByQuery(@RequestParam @NotBlank @Size(max = 5000) String question) {
        return chatService.ask(null, question);
    }

    public record LegacyChatRequest(String conversationId,
                                    @NotBlank @Size(max = 5000) String question) {
    }
}
