package max.example.spring_ai_rag_demo.checklist;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.stereotype.Service;

@Service
public class ChecklistKnowledgeBaseRetriever {

    private final VectorStoreRetriever retriever;

    public ChecklistKnowledgeBaseRetriever(VectorStoreRetriever retriever) {
        this.retriever = retriever;
    }

    public List<Document> retrieve(String question) {
        return retriever.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(3)
                        .build()
        );
    }
}
