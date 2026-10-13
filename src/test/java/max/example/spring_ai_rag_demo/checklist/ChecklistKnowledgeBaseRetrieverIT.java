package max.example.spring_ai_rag_demo.checklist;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "knowledge.vector-store.ingestion.enabled=true"
})
class ChecklistKnowledgeBaseRetrieverIT {

    @Autowired
    private ChecklistKnowledgeBaseRetriever retriever;

    @Test
    void shouldRetrieveDocumentsFromVectorStore() {
        String question = "Как правильно передать отправление клиенту?";

        List<Document> documents = retriever.retrieve(question);

        assertFalse(documents.isEmpty());

        documents.forEach(document -> {
            assertNotNull(document.getId());
            assertFalse(document.getId().isBlank());

            assertNotNull(document.getMetadata().get("questionId"));
            assertNotNull(document.getMetadata().get("type"));
        });
    }
}
