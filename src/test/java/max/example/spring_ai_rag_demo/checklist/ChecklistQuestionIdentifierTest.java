
package max.example.spring_ai_rag_demo.checklist;

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
class ChecklistQuestionIdentifierTest {

    @Mock
    private QuestionSemanticMatcher semanticMatcher;

    @Test
    void shouldReturnNotIdentifiedWhenCandidatesAreEmpty() {
        var identifier = new ChecklistQuestionIdentifier(semanticMatcher);

        var result = identifier.identify(
                NormalizedChecklistQuestion.textOnly("Что проверить?"),
                List.of()
        );

        assertInstanceOf(
                QuestionIdentificationResult.NotIdentified.class,
                result
        );

        var notIdentified =
                (QuestionIdentificationResult.NotIdentified) result;

        assertEquals(
                QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                notIdentified.reason()
        );
        assertTrue(notIdentified.candidateQuestionIds().isEmpty());

        verifyNoInteractions(semanticMatcher);
    }

    @Test
    void shouldReturnIdentifiedWhenMatcherSelectsRetrievedCandidate() {
        var identifier = new ChecklistQuestionIdentifier(semanticMatcher);
        var question = NormalizedChecklistQuestion.textOnly(
                "Что нужно проверить перед передачей заказа?"
        );
        var candidates = List.of(
                document("Q-001", "Проверить комплектность заказа"),
                document("Q-002", "Проверить внешний вид курьера")
        );

        when(semanticMatcher.match(question, candidates))
                .thenReturn(new QuestionIdentificationResult.Identified(
                        "Q-001",
                        List.of("Q-001", "Q-002"),
                        OptionalDouble.of(0.95)
                ));

        var result = identifier.identify(question, candidates);

        assertInstanceOf(
                QuestionIdentificationResult.Identified.class,
                result
        );
        assertEquals(
                "Q-001",
                ((QuestionIdentificationResult.Identified) result)
                        .questionId()
        );

        verify(semanticMatcher).match(question, candidates);
    }

    @Test
    void shouldReturnAmbiguousWhenMatcherCannotChooseOneCandidate() {
        var identifier = new ChecklistQuestionIdentifier(semanticMatcher);
        var question = NormalizedChecklistQuestion.textOnly(
                "Что нужно проверить?"
        );
        var candidates = List.of(
                document("Q-001", "Проверить комплектность"),
                document("Q-002", "Проверить оборудование")
        );

        when(semanticMatcher.match(question, candidates))
                .thenReturn(new QuestionIdentificationResult.Ambiguous(
                        List.of("Q-001", "Q-002"),
                        QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES,
                        OptionalDouble.empty()
                ));

        var result = identifier.identify(question, candidates);

        assertInstanceOf(
                QuestionIdentificationResult.Ambiguous.class,
                result
        );
    }

    @Test
    void shouldPreserveNotIdentifiedResultFromMatcher() {
        var identifier = new ChecklistQuestionIdentifier(semanticMatcher);
        var question = NormalizedChecklistQuestion.textOnly(
                "Куда обращаться при поломке автомобиля?"
        );
        var candidates = List.of(
                document("Q-001", "Проверить комплектность заказа")
        );

        when(semanticMatcher.match(question, candidates))
                .thenReturn(new QuestionIdentificationResult.NotIdentified(
                        List.of("Q-001"),
                        QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                        OptionalDouble.empty()
                ));

        var result = identifier.identify(question, candidates);

        assertInstanceOf(
                QuestionIdentificationResult.NotIdentified.class,
                result
        );
    }

    @Test
    void shouldRejectIdentifiedQuestionOutsideRetrievedCandidates() {
        var identifier = new ChecklistQuestionIdentifier(semanticMatcher);
        var question = NormalizedChecklistQuestion.textOnly(
                "Что нужно проверить?"
        );
        var candidates = List.of(
                document("Q-001", "Проверить комплектность")
        );

        when(semanticMatcher.match(question, candidates))
                .thenReturn(new QuestionIdentificationResult.Identified(
                        "Q-999",
                        List.of("Q-999"),
                        OptionalDouble.of(0.99)
                ));

        var result = identifier.identify(question, candidates);

        assertInstanceOf(
                QuestionIdentificationResult.NotIdentified.class,
                result
        );

        var notIdentified =
                (QuestionIdentificationResult.NotIdentified) result;

        assertEquals(
                List.of("Q-001"),
                notIdentified.candidateQuestionIds()
        );
        assertEquals(
                QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                notIdentified.reason()
        );
    }

    @Test
    void shouldRejectAmbiguousResultContainingUnknownCandidate() {
        var identifier = new ChecklistQuestionIdentifier(semanticMatcher);
        var question = NormalizedChecklistQuestion.textOnly(
                "Что нужно проверить?"
        );
        var candidates = List.of(
                document("Q-001", "Проверить комплектность"),
                document("Q-002", "Проверить оборудование")
        );

        when(semanticMatcher.match(question, candidates))
                .thenReturn(new QuestionIdentificationResult.Ambiguous(
                        List.of("Q-001", "Q-999"),
                        QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES,
                        OptionalDouble.empty()
                ));

        var result = identifier.identify(question, candidates);

        assertInstanceOf(
                QuestionIdentificationResult.NotIdentified.class,
                result
        );
    }

    @Test
    void shouldPassAnswerOptionsToSemanticMatcher() {
        var identifier = new ChecklistQuestionIdentifier(semanticMatcher);
        var question = new NormalizedChecklistQuestion(
                "Что запрещено делать?",
                List.of("Оставлять заказ у двери", "Передавать заказ лично"),
                List.of("Оставлять заказ у двери")
        );
        var candidates = List.of(
                document("Q-001", "Какие действия запрещены при доставке?")
        );

        when(semanticMatcher.match(question, candidates))
                .thenReturn(new QuestionIdentificationResult.Identified(
                        "Q-001",
                        List.of("Q-001"),
                        OptionalDouble.empty()
                ));

        identifier.identify(question, candidates);

        verify(semanticMatcher).match(question, candidates);
    }

    private static Document document(String id, String text) {
        return new Document(id, text, Map.of());
    }
}
