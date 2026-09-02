package max.example.spring_ai_rag_demo.checklist;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(
        name = "knowledge.vector-store.ingestion.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ChecklistKnowledgeBaseVectorStoreLoader {

    private final ChecklistKnowledgeBaseLoader knowledgeBaseLoader;
    private final VectorStore vectorStore;

    public ChecklistKnowledgeBaseVectorStoreLoader(
            ChecklistKnowledgeBaseLoader knowledgeBaseLoader,
            VectorStore vectorStore
    ) {
        this.knowledgeBaseLoader = knowledgeBaseLoader;
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        ChecklistKnowledgeBase knowledgeBase = knowledgeBaseLoader.load();

        List<Document> documents = knowledgeBase.questions().stream()
                .map(this::toDocument)
                .toList();

        vectorStore.add(documents);
    }

    private Document toDocument(ChecklistQuestion question) {
        String content = question.question()
                + "\n"
                + question.options().stream()
                .map(option -> option.id() + ") " + option.text())
                .reduce((first, second) -> first + "\n" + second)
                .orElse("");

        return new Document(
                content,
                Map.of(
                        "questionId", question.questionId(),
                        "type", question.type()
                )
        );
    }
}
