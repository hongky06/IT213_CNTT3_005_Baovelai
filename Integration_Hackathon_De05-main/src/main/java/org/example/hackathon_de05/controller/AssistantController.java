package org.example.hackathon_de05.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.hackathon_de05.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final ChatService chatService;

    public AssistantController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/ask")
    public ChatService.ChatResult ask(@Valid @RequestBody AskRequest request) {
        return chatService.ask(request.conversationId(), request.question());
    }

    public record AskRequest(String conversationId,
                             @NotBlank @Size(max = 5000) String question) {
    }
}
