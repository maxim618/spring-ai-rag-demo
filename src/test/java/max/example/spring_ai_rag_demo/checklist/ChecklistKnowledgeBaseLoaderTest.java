package max.example.spring_ai_rag_demo.checklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ChecklistKnowledgeBaseLoaderTest {

    @Test
    void shouldLoadChecklistKnowledgeBase() {
        ChecklistKnowledgeBaseLoader loader = new ChecklistKnowledgeBaseLoader(
                new ObjectMapper(),
                Path.of("data/checklist-knowledge-base.example.json").toString()
        );

        ChecklistKnowledgeBase knowledgeBase = loader.load();

        assertThat(knowledgeBase.questions()).hasSize(3);
        assertThat(knowledgeBase.questions().get(0).questionId()).isEqualTo("Q-001");
        assertThat(knowledgeBase.questions().get(0).options()).hasSize(4);
        assertThat(knowledgeBase.questions().get(0).correctOptions()).containsExactly("A");
        assertThat(knowledgeBase.questions().get(1).type()).isEqualTo("MULTIPLE_CHOICE");
        assertThat(knowledgeBase.questions().get(1).correctOptions()).containsExactly("A", "C");
    }
}
