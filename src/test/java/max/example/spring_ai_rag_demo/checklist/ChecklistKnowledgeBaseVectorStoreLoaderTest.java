package max.example.spring_ai_rag_demo.checklist;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChecklistKnowledgeBaseVectorStoreLoaderTest {

    @Test
    void shouldLoadChecklistKnowledgeBaseIntoVectorStore() {
        ChecklistKnowledgeBase knowledgeBase = new ChecklistKnowledgeBase(List.of(
                new ChecklistQuestion(
                        "Q-001",
                        "SINGLE_CHOICE",
                        "Что необходимо сделать перед началом рабочего дня?",
                        List.of(
                                new ChecklistOption("A", "Проверить рабочее оборудование"),
                                new ChecklistOption("B", "Пропустить проверку оборудования")
                        ),
                        List.of("A")
                )
        ));

        ChecklistKnowledgeBaseLoader knowledgeBaseLoader = mock(ChecklistKnowledgeBaseLoader.class);
        VectorStore vectorStore = mock(VectorStore.class);

        org.mockito.Mockito.when(knowledgeBaseLoader.load()).thenReturn(knowledgeBase);

        ChecklistKnowledgeBaseVectorStoreLoader loader = new ChecklistKnowledgeBaseVectorStoreLoader(
                knowledgeBaseLoader,
                vectorStore
        );

        loader.load();

        var documentsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(documentsCaptor.capture());

        List<Document> documents = documentsCaptor.getValue();

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getText())
                .contains("Что необходимо сделать перед началом рабочего дня?")
                .contains("A) Проверить рабочее оборудование")
                .contains("B) Пропустить проверку оборудования");
        assertThat(documents.get(0).getMetadata())
                .containsEntry("questionId", "Q-001")
                .containsEntry("type", "SINGLE_CHOICE");
    }
}
