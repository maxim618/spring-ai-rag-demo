
package max.example.spring_ai_rag_demo.checklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(OutputCaptureExtension.class)
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
        when(responseSpec.content()).thenReturn("""
                {"status":"IDENTIFIED","questionId":"Q-001","candidateQuestionIds":["Q-001"],"reason":null,"confidence":0.95}
                """);

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
        when(responseSpec.content()).thenReturn("""
                {"status":"AMBIGUOUS","questionId":null,"candidateQuestionIds":["Q-001","Q-002"],"reason":"MULTIPLE_CANDIDATES","confidence":0.65}
                """);

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
        when(responseSpec.content()).thenReturn("""
                {"status":"NOT_IDENTIFIED","questionId":null,"candidateQuestionIds":[],"reason":"NO_RELIABLE_MATCH","confidence":0.2}
                """);

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly("Как оформить отпуск?"),
                List.of(document("Q-001", "Проверьте упаковку"))
        );

        assertInstanceOf(QuestionIdentificationResult.NotIdentified.class, result);
    }

    @Test
    void shouldReturnNotIdentifiedWhenReasonIsMultipleCandidates() {
        when(responseSpec.content()).thenReturn("""
                {"status":"NOT_IDENTIFIED","questionId":null,"candidateQuestionIds":[],"reason":"MULTIPLE_CANDIDATES","confidence":0.2}
                """);

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
        when(responseSpec.content()).thenReturn("""
                {"status":"IDENTIFIED","questionId":"Q-001","candidateQuestionIds":["Q-001",null],"reason":null,"confidence":0.9}
                """);

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly("Как проверить упаковку?"),
                List.of(document("Q-001", "Проверьте упаковку"))
        );

        assertInstanceOf(QuestionIdentificationResult.NotIdentified.class, result);
    }


    @Test
    void shouldRejectInventedQuestionId() {
        when(responseSpec.content()).thenReturn("""
                {"status":"IDENTIFIED","questionId":"INVENTED-ID","candidateQuestionIds":["INVENTED-ID"],"reason":null,"confidence":0.99}
                """);

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly("Как проверить документ?"),
                List.of(document("Q-001", "Проверьте документ"))
        );

        assertInstanceOf(QuestionIdentificationResult.NotIdentified.class, result);
    }

    @Test
    void shouldRejectInvalidConfidence() {
        when(responseSpec.content()).thenReturn("""
                {"status":"IDENTIFIED","questionId":"Q-001","candidateQuestionIds":["Q-001"],"reason":null,"confidence":1.5}
                """);

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly("Как проверить документ?"),
                List.of(document("Q-001", "Проверьте документ"))
        );

        assertInstanceOf(QuestionIdentificationResult.NotIdentified.class, result);
    }

    @Test
    void shouldReturnNotIdentifiedWhenAmbiguousHasLessThanTwoCandidates() {
        when(responseSpec.content()).thenReturn("""
                {"status":"AMBIGUOUS","questionId":null,"candidateQuestionIds":["Q-001"],"reason":"MULTIPLE_CANDIDATES","confidence":0.65}
                """);

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly("Как проверить отправление?"),
                List.of(document("Q-001", "Проверьте упаковку"))
        );

        assertInstanceOf(QuestionIdentificationResult.NotIdentified.class, result);
    }

    @Test
    void shouldReturnNotIdentifiedWhenReasonIsUnknown() {
        when(responseSpec.content()).thenReturn("""
                {"status":"NOT_IDENTIFIED","questionId":null,"candidateQuestionIds":[],"reason":"UNKNOWN_REASON","confidence":0.65}
                """);

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly("Как проверить отправление?"),
                List.of(document("Q-001", "Проверьте упаковку"))
        );

        var notIdentified = assertInstanceOf(
                QuestionIdentificationResult.NotIdentified.class,
                result
        );

        assertEquals(
                QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                notIdentified.reason()
        );
    }

    @Test
    void shouldWrapNullLlmResponse() {
        when(responseSpec.content()).thenReturn("null");

        assertThrows(
                QuestionIdentificationException.class,
                () -> matcher.match(
                        NormalizedChecklistQuestion.textOnly(
                                "Что необходимо сделать?"
                        ),
                        List.of(document(
                                "Q-001",
                                "Что проверить перед началом рабочего дня?"
                        ))
                )
        );
    }

    @Test
    void shouldReturnNotIdentifiedWhenNegationChangesMeaning() {
        when(responseSpec.content()).thenReturn("""
                {"status":"NOT_IDENTIFIED","questionId":null,"candidateQuestionIds":[],"reason":"NEGATION_MISMATCH","confidence":0.65}
                """);

        var result = matcher.match(
                NormalizedChecklistQuestion.textOnly(
                        "Нельзя передавать отправление без подписи получателя?"
                ),
                List.of(document(
                        "Q-001",
                        "Можно ли передавать отправление без подписи получателя?"
                ))
        );

        var notIdentified = assertInstanceOf(
                QuestionIdentificationResult.NotIdentified.class,
                result
        );

        assertEquals(
                QuestionIdentificationResult.Reason.NEGATION_MISMATCH,
                notIdentified.reason()
        );
    }

    @Test
    void shouldLogLlmFailureAndPreserveOriginalCause(CapturedOutput output) {
        RuntimeException cause = new RuntimeException("Invalid JSON from LLM");

        when(responseSpec.content()).thenThrow(cause);

        QuestionIdentificationException exception = assertThrows(
                QuestionIdentificationException.class,
                () -> matcher.match(
                        NormalizedChecklistQuestion.textOnly(
                                "Что необходимо сделать?"
                        ),
                        List.of(document(
                                "Q-001",
                                "Что проверить перед началом рабочего дня?"
                        ))
                )
        );

        assertSame(cause, exception.getCause());

        assertTrue(output.getAll()
                .contains("Question identification failed during LLM call or response conversion"));

        assertTrue(output.getAll().contains("Invalid JSON from LLM"));
    }

    private static Document document(String questionId, String text) {
        return new Document(text, Map.of("questionId", questionId));
    }
}
