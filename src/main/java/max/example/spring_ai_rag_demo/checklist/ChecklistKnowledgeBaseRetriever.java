package max.example.spring_ai_rag_demo.checklist;

import java.util.List;

import com.openai.errors.RateLimitException;
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
        try {
            return retriever.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(3)
                            .build()
            );
        } catch (RateLimitException exception) {
            throw new EmbeddingProviderRateLimitException(exception);
        }
    }
}
