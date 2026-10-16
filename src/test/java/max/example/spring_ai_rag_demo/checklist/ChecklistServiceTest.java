package max.example.spring_ai_rag_demo.checklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistServiceTest {

    @Mock
    private ChecklistKnowledgeBaseRetriever retriever;

    @Mock
    private ChecklistQuestionIdentifier identifier;

    @Mock
    private ChecklistKnowledgeBaseLoader knowledgeBaseLoader;

    private ChecklistService service;

    @BeforeEach
    void setUp() {
        when(knowledgeBaseLoader.load()).thenReturn(new ChecklistKnowledgeBase(List.of(
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
        )));
        service = new ChecklistService(retriever, identifier, knowledgeBaseLoader);
    }

    @Test
    void shouldReturnCanonicalQuestionWithoutCorrectOptions() {
        var input = NormalizedChecklistQuestion.textOnly(
                "Что проверить перед началом смены?"
        );
        var candidates = List.of(document("Q-001"));
        when(retriever.retrieve(input.text())).thenReturn(candidates);
        when(identifier.identify(input, candidates)).thenReturn(
                new QuestionIdentificationResult.Identified(
                        "Q-001", List.of("Q-001"), OptionalDouble.of(0.95)
                )
        );

        var result = service.identifyQuestion(input);

        assertInstanceOf(ChecklistQuestionLookupResult.Identified.class, result);
        var identified = (ChecklistQuestionLookupResult.Identified) result;
        assertEquals("Q-001", identified.questionId());
        assertEquals("Что необходимо сделать перед началом рабочего дня?", identified.question());
        assertEquals(List.of(new ChecklistOption("A", "Проверить рабочее оборудование"),
                new ChecklistOption("B", "Пропустить проверку оборудования")), identified.options());
        verify(retriever).retrieve(input.text());
        verify(identifier).identify(input, candidates);
    }

    @Test
    void shouldReturnAmbiguousWithoutLookingUpAnAnswer() {
        var input = NormalizedChecklistQuestion.textOnly("Что проверить?");
        var candidates = List.of(document("Q-001"), document("Q-002"));
        when(retriever.retrieve(input.text())).thenReturn(candidates);
        when(identifier.identify(input, candidates)).thenReturn(
                new QuestionIdentificationResult.Ambiguous(
                        List.of("Q-001", "Q-002"),
                        QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES,
                        OptionalDouble.empty()
                )
        );

        var result = service.identifyQuestion(input);

        assertEquals(
                new ChecklistQuestionLookupResult.Ambiguous(
                        List.of("Q-001", "Q-002"),
                        QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES,
                        OptionalDouble.empty()
                ),
                result
        );
    }

    @Test
    void shouldPreserveNotIdentifiedResult() {
        var input = NormalizedChecklistQuestion.textOnly("Вопрос вне базы");
        when(retriever.retrieve(input.text())).thenReturn(List.of());
        when(identifier.identify(input, List.of())).thenReturn(
                new QuestionIdentificationResult.NotIdentified(
                        List.of(),
                        QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                        OptionalDouble.empty()
                )
        );

        var result = service.identifyQuestion(input);

        assertInstanceOf(ChecklistQuestionLookupResult.NotIdentified.class, result);
    }

    @Test
    void shouldFailClosedWhenIdentifiedIdIsAbsentFromKnowledgeBase() {
        var input = NormalizedChecklistQuestion.textOnly("Вопрос");
        var candidates = List.of(document("Q-999"));
        when(retriever.retrieve(input.text())).thenReturn(candidates);
        when(identifier.identify(input, candidates)).thenReturn(
                new QuestionIdentificationResult.Identified(
                        "Q-999", List.of("Q-999"), OptionalDouble.of(0.99)
                )
        );

        var result = service.identifyQuestion(input);

        assertInstanceOf(ChecklistQuestionLookupResult.NotIdentified.class, result);
        assertEquals(
                QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                ((ChecklistQuestionLookupResult.NotIdentified) result).reason()
        );
    }

    @Test
    void shouldRejectDuplicateQuestionIdsInKnowledgeBase() {
        var question = new ChecklistQuestion(
                "Q-001", "SINGLE_CHOICE", "Вопрос",
                List.of(new ChecklistOption("A", "Вариант")), List.of("A")
        );
        when(knowledgeBaseLoader.load()).thenReturn(
                new ChecklistKnowledgeBase(List.of(question, question))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ChecklistService(retriever, identifier, knowledgeBaseLoader)
        );
    }

    private static Document document(String questionId) {
        return new Document(
                "document-" + questionId,
                "Candidate question",
                Map.of("questionId", questionId)
        );
    }
}
