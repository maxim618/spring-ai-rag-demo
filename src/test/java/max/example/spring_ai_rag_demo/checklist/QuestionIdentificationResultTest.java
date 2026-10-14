
package max.example.spring_ai_rag_demo.checklist;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.OptionalDouble;

class QuestionIdentificationResultTest {

    @Test
    void shouldCreateIdentifiedResult() {
        var result = new QuestionIdentificationResult(
                QuestionIdentificationResult.Status.IDENTIFIED,
                "Q-001",
                List.of("Q-001"),
                OptionalDouble.empty(),
                QuestionIdentificationResult.Reason.NONE
        );

        assertEquals(
                QuestionIdentificationResult.Status.IDENTIFIED,
                result.status()
        );
        assertEquals("Q-001", result.questionId());
    }

    @Test
    void shouldCreateAmbiguousResult() {
        var result = new QuestionIdentificationResult(
                QuestionIdentificationResult.Status.AMBIGUOUS,
                null,
                List.of("Q-001", "Q-002"),
                OptionalDouble.empty(),
                QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES
        );

        assertNull(result.questionId());
        assertEquals(2, result.candidateQuestionIds().size());
    }

    @Test
    void shouldCreateNotIdentifiedResultWithRejectedCandidates() {
        var result = new QuestionIdentificationResult(
                QuestionIdentificationResult.Status.NOT_IDENTIFIED,
                null,
                List.of("Q-001"),
                OptionalDouble.empty(),
                QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH
        );

        assertNull(result.questionId());
        assertEquals(
                QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                result.reason()
        );
    }

    @Test
    void shouldRejectIdentifiedResultWithoutQuestionId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult(
                        QuestionIdentificationResult.Status.IDENTIFIED,
                        null,
                        List.of(),
                        OptionalDouble.empty(),
                        QuestionIdentificationResult.Reason.NONE
                )
        );
    }

    @Test
    void shouldRejectAmbiguousResultWithOnlyOneCandidate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult(
                        QuestionIdentificationResult.Status.AMBIGUOUS,
                        null,
                        List.of("Q-001"),
                        OptionalDouble.empty(),
                        QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES
                )
        );
    }

    @Test
    void shouldRejectAmbiguousResultWithSelectedQuestionId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult(
                        QuestionIdentificationResult.Status.AMBIGUOUS,
                        "Q-001",
                        List.of("Q-001", "Q-002"),
                        OptionalDouble.empty(),
                        QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES
                )
        );
    }

    @Test
    void shouldRejectNotIdentifiedResultWithSelectedQuestionId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult(
                        QuestionIdentificationResult.Status.NOT_IDENTIFIED,
                        "Q-001",
                        List.of("Q-001"),
                        OptionalDouble.empty(),
                        QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH
                )
        );
    }

    @Test
    void shouldRejectConfidenceOutsideRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult(
                        QuestionIdentificationResult.Status.IDENTIFIED,
                        "Q-001",
                        List.of("Q-001"),
                        OptionalDouble.of(1.1),
                        QuestionIdentificationResult.Reason.NONE
                )
        );
    }

    @Test
    void shouldRepresentUnavailableConfidenceExplicitly() {
        var result = new QuestionIdentificationResult(
                QuestionIdentificationResult.Status.IDENTIFIED,
                "Q-001",
                List.of("Q-001"),
                OptionalDouble.empty(),
                QuestionIdentificationResult.Reason.NONE
        );

        assertTrue(result.confidence().isEmpty());
    }

    @Test
    void shouldAcceptCalculatedConfidence() {
        var result = new QuestionIdentificationResult(
                QuestionIdentificationResult.Status.IDENTIFIED,
                "Q-001",
                List.of("Q-001"),
                OptionalDouble.of(0.95),
                QuestionIdentificationResult.Reason.NONE
        );

        assertEquals(0.95, result.confidence().orElseThrow());
    }
}
