package org.example.hackathon_de05.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.example.hackathon_de05.tools.ParkingReservationTools;
import org.example.hackathon_de05.tools.PolicyRagTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory,
                                 ParkingReservationTools parkingReservationTools, PolicyRagTool policyRagTool) {
        return chatClientBuilder
                .defaultSystem("""
                        Bạn là trợ lý đặt chỗ đỗ xe. Dùng các công cụ Java khi cần dữ liệu nghiệp vụ.
                        Không tự suy đoán tình trạng chỗ, người dùng, requestId hoặc kết quả thao tác.
                        Luôn dùng searchInternalPolicies cho câu hỏi về chính sách và chỉ trả lời dựa trên
                        bằng chứng công cụ trả về. Nếu công cụ trả về đúng câu "Không đủ căn cứ trong tài liệu nội bộ.",
                        hãy trả nguyên văn câu đó. Chỉ xác nhận tạo yêu cầu sau khi công cụ tạo yêu cầu thành công
                        và phải nêu requestId.
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(parkingReservationTools, policyRagTool)
                .build();
    }
}
