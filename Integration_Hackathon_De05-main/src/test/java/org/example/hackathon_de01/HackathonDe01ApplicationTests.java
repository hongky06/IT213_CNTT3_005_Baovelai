package org.example.hackathon_de01;

import org.example.hackathon_de05.HackathonDe05Application;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = HackathonDe05Application.class)
@ActiveProfiles("test")
class HackathonDe05ApplicationTests {
    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private VectorStore vectorStore;

    @Autowired
    private HackathonDe05Application application;

    @Test
    void contextLoadsWithTheTestDatabaseAndExternalServicesIsolated() {
        assertNotNull(application);
    }
}
