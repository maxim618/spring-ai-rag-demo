package max.example.spring_ai_rag_demo.checklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class ChecklistKnowledgeBaseLoader {

    private final ObjectMapper objectMapper;
    private final Path knowledgeFile;

    public ChecklistKnowledgeBaseLoader(
            ObjectMapper objectMapper,
            @Value("${knowledge.file}") String knowledgeFile
    ) {
        this.objectMapper = objectMapper;
        this.knowledgeFile = Path.of(knowledgeFile);
    }

    public ChecklistKnowledgeBase load() {
        try {
            return objectMapper.readValue(
                    knowledgeFile.toFile(),
                    ChecklistKnowledgeBase.class
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load checklist knowledge base: " + knowledgeFile,
                    e
            );
        }
    }
}
