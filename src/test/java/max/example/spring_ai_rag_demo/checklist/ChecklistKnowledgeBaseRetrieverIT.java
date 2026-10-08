package max.example.spring_ai_rag_demo.checklist;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
            System.out.println("-----");
            System.out.println("Content:");
            System.out.println(document.getText());
            System.out.println("Metadata:");
            System.out.println(document.getMetadata());
            System.out.println("ID: " + document.getId());
        });
    }
}