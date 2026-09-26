package org.example.hackathon_de05.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.DefaultResourceLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RAGServiceTest {

    @Test
    void ingestsTheProvidedCorpusOnceWithStableIdentifiersAndSectionMetadata() throws Exception {
        VectorStore vectorStore = mock(VectorStore.class);
        RAGService service = service(vectorStore, new ChatExecutionContext());
        AtomicReference<List<Document>> documents = new AtomicReference<>();
        doAnswer(invocation -> {
            documents.set(invocation.getArgument(0));
            return null;
        }).when(vectorStore).add(anyList());

        int firstIngest = service.ingestPolicyCorpus();
        int repeatedIngest = service.ingestPolicyCorpus();

        verify(vectorStore).add(anyList());
        List<Document> ingested = documents.get();
        assertEquals(ingested.size(), firstIngest);
        assertEquals(0, repeatedIngest);
        assertFalse(ingested.isEmpty());
        assertEquals(ingested.size(), ingested.stream().map(Document::getId).distinct().count());
        assertEquals("tai_lieu_noi_bo.md", ingested.get(0).getMetadata().get("source"));
        assertEquals("Điều kiện tạo yêu cầu", ingested.get(0).getMetadata().get("section"));
    }

    @Test
    void returnsExactFallbackWhenNoPolicyDocumentMatches() throws Exception {
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        ChatExecutionContext context = new ChatExecutionContext();
        RAGService service = service(vectorStore, context);
        try (ChatExecutionContext.Scope ignored = context.open("conversation-1")) {
            RAGService.RetrievalResult result = service.retrieve("unsupported policy question");
            assertEquals(RAGService.FALLBACK_ANSWER, result.evidence());
            assertEquals(List.of(), result.sources());
        }
    }

    @Test
    void includesSourceMetadataForRetrievedEvidence() throws Exception {
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("source-1", "Nguồn chính sách được tìm thấy.",
                        Map.of("section", "Duyệt yêu cầu", "source", "tai_lieu_noi_bo.md"))));
        ChatExecutionContext context = new ChatExecutionContext();
        RAGService service = service(vectorStore, context);
        try (ChatExecutionContext.Scope scope = context.open("conversation-1")) {
            RAGService.RetrievalResult result = service.retrieve("approval policy");
            assertEquals(1, result.sources().size());
            assertEquals("Duyệt yêu cầu", result.sources().get(0).section());
            assertEquals("tai_lieu_noi_bo.md", result.sources().get(0).source());
            assertEquals(result.sources(), scope.sources());
        }
    }

    private RAGService service(VectorStore vectorStore, ChatExecutionContext context) {
        return new RAGService(vectorStore, new DefaultResourceLoader(), context, 0.70);
    }

}
