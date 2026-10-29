package max.example.spring_ai_rag_demo.checklist;

public class EmbeddingProviderRateLimitException extends RuntimeException {

    public EmbeddingProviderRateLimitException(Throwable cause) {
        super("Embedding provider rate limit exceeded", cause);
    }
}
