package max.example.spring_ai_rag_demo.checklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;

class ChecklistKnowledgeBaseRetrieverTest {

    private final VectorStoreRetriever retriever =
            mock(VectorStoreRetriever.class);

    private final ChecklistKnowledgeBaseRetriever knowledgeBaseRetriever =
            new ChecklistKnowledgeBaseRetriever(retriever);

    @Test
    void shouldRetrieveTopThreeDocuments() {
        String question = "Как правильно передать отправление клиенту?";

        List<Document> expected = List.of(
                new Document("Document 1"),
                new Document("Document 2"),
                new Document("Document 3")
        );

        when(retriever.similaritySearch(any(SearchRequest.class)))
                .thenReturn(expected);

        List<Document> actual = knowledgeBaseRetriever.retrieve(question);

        assertEquals(expected, actual);

        ArgumentCaptor<SearchRequest> requestCaptor =
                ArgumentCaptor.forClass(SearchRequest.class);

        verify(retriever).similaritySearch(requestCaptor.capture());

        SearchRequest request = requestCaptor.getValue();

        assertEquals(question, request.getQuery());
        assertEquals(3, request.getTopK());
    }
}
