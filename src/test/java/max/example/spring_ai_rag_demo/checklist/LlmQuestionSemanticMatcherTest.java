
package max.example.spring_ai_rag_demo.checklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LlmQuestionSemanticMatcherTest {

    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;

    private LlmQuestionSemanticMatcher matcher;

    @BeforeEach
    void setUp() {
        chatClientBuilder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        matcher = new LlmQuestionSemanticMatcher(chatClientBuilder);
    }

    @Test
    void shouldIdentifyMatchingQuestion() {
        when(responseSpec.entity(
                LlmQuestionSemanticMatcher.MatchDecision.class
        )).thenReturn(new LlmQuestionSemanticMatcher.MatchDecision(
                LlmQuestionSemanticMatcher.Status.IDENTIFIED,
                "Q-001",
                List.of("Q-001"),
                null,
                0.95
        ));

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly("Как проверить документ?"),
                List.of(document("Q-001", "Проверьте документ перед передачей"))
        );

        assertInstanceOf(QuestionIdentificationResult.Identified.class, result);
        var identified = (QuestionIdentificationResult.Identified) result;
        assertEquals("Q-001", identified.questionId());
        assertEquals(0.95, identified.confidence().orElseThrow());
    }

    @Test
    void shouldReturnAmbiguousWhenSeveralCandidatesFit() {
        when(responseSpec.entity(
                LlmQuestionSemanticMatcher.MatchDecision.class
        )).thenReturn(new LlmQuestionSemanticMatcher.MatchDecision(
                LlmQuestionSemanticMatcher.Status.AMBIGUOUS,
                null,
                List.of("Q-001", "Q-002"),
                "MULTIPLE_CANDIDATES",
                0.65
        ));

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly("Как проверить отправление?"),
                List.of(
                        document("Q-001", "Проверьте упаковку"),
                        document("Q-002", "Проверьте маркировку")
                )
        );

        assertInstanceOf(QuestionIdentificationResult.Ambiguous.class, result);
    }

    @Test
    void shouldReturnNotIdentifiedWhenNoCandidateFits() {
        when(responseSpec.entity(
                LlmQuestionSemanticMatcher.MatchDecision.class
        )).thenReturn(new LlmQuestionSemanticMatcher.MatchDecision(
                LlmQuestionSemanticMatcher.Status.NOT_IDENTIFIED,
                null,
                List.of(),
                "NO_RELIABLE_MATCH",
                0.2
        ));

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly("Как оформить отпуск?"),
                List.of(document("Q-001", "Проверьте упаковку"))
        );

        assertInstanceOf(QuestionIdentificationResult.NotIdentified.class, result);
    }

    @Test
    void shouldReturnNotIdentifiedWhenReasonIsMultipleCandidates() {
        when(responseSpec.entity(
                LlmQuestionSemanticMatcher.MatchDecision.class
        )).thenReturn(new LlmQuestionSemanticMatcher.MatchDecision(
                LlmQuestionSemanticMatcher.Status.NOT_IDENTIFIED,
                null,
                List.of(),
                "MULTIPLE_CANDIDATES",
                0.2
        ));

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly("Как оформить отпуск?"),
                List.of(document("Q-001", "Проверьте упаковку"))
        );

        assertInstanceOf(QuestionIdentificationResult.NotIdentified.class, result);

        var notIdentified = (QuestionIdentificationResult.NotIdentified) result;
        assertEquals(
                QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                notIdentified.reason()
        );
    }

    @Test
    void shouldReturnNotIdentifiedWhenCandidateIdsContainNull() {
        when(responseSpec.entity(
                LlmQuestionSemanticMatcher.MatchDecision.class
        )).thenReturn(new LlmQuestionSemanticMatcher.MatchDecision(
                LlmQuestionSemanticMatcher.Status.IDENTIFIED,
                "Q-001",
                Arrays.asList("Q-001", null),
                null,
                0.9
        ));

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly("Как проверить упаковку?"),
                List.of(document("Q-001", "Проверьте упаковку"))
        );

        assertInstanceOf(QuestionIdentificationResult.NotIdentified.class, result);
    }


    @Test
    void shouldRejectInventedQuestionId() {
        when(responseSpec.entity(
                LlmQuestionSemanticMatcher.MatchDecision.class
        )).thenReturn(new LlmQuestionSemanticMatcher.MatchDecision(
                LlmQuestionSemanticMatcher.Status.IDENTIFIED,
                "INVENTED-ID",
                List.of("INVENTED-ID"),
                null,
                0.99
        ));

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly("Как проверить документ?"),
                List.of(document("Q-001", "Проверьте документ"))
        );

        assertInstanceOf(QuestionIdentificationResult.NotIdentified.class, result);
    }

    @Test
    void shouldRejectInvalidConfidence() {
        when(responseSpec.entity(
                LlmQuestionSemanticMatcher.MatchDecision.class
        )).thenReturn(new LlmQuestionSemanticMatcher.MatchDecision(
                LlmQuestionSemanticMatcher.Status.IDENTIFIED,
                "Q-001",
                List.of("Q-001"),
                null,
                1.5
        ));

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly("Как проверить документ?"),
                List.of(document("Q-001", "Проверьте документ"))
        );

        assertInstanceOf(QuestionIdentificationResult.NotIdentified.class, result);
    }

    private static Document document(String questionId, String text) {
        return new Document(text, Map.of("questionId", questionId));
    }
}
