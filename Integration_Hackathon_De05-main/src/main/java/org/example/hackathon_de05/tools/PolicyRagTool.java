package org.example.hackathon_de05.tools;

import org.example.hackathon_de05.service.ChatExecutionContext;
import org.example.hackathon_de05.service.RAGService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PolicyRagTool {
    private final RAGService ragService;
    private final ChatExecutionContext executionContext;

    public PolicyRagTool(RAGService ragService, ChatExecutionContext executionContext) {
        this.ragService = ragService;
        this.executionContext = executionContext;
    }

    @Tool(name = "searchInternalPolicies",
            description = "Tra cứu corpus chính sách nội bộ. Phải dùng cho câu hỏi chính sách; không suy đoán ngoài bằng chứng trả về.")
    public String searchInternalPolicies(
            @ToolParam(description = "Câu hỏi cần tìm trong tài liệu nội bộ") String question) throws IOException {
        executionContext.recordTool("searchInternalPolicies");
        return ragService.retrieve(question).evidence();
    }
}
