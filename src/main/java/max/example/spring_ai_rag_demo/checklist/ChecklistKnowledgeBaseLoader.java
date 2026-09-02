package max.example.spring_ai_rag_demo.checklist;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class ChecklistKnowledgeBaseLoader {

    private final JsonMapper jsonMapper;
    private final Path knowledgeFile;

    public ChecklistKnowledgeBaseLoader(
            JsonMapper jsonMapper,
            @Value("${knowledge.file}") String knowledgeFile
    ) {
        this.jsonMapper = jsonMapper;
        this.knowledgeFile = Path.of(knowledgeFile);
    }

    public ChecklistKnowledgeBase load() {
        try {
            return jsonMapper.readValue(
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
