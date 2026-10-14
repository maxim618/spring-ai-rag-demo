
package max.example.spring_ai_rag_demo.checklist;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionIdentificationResultTest {

    @Test
    void shouldCreateIdentifiedResult() {
        var result = new QuestionIdentificationResult(
                QuestionIdentificationResult.Status.IDENTIFIED,
                "Q-001",
                List.of("Q-001"),
                null,
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
                null,
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
                null,
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
                        null,
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
                        null,
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
                        null,
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
                        null,
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
                        1.1,
                        QuestionIdentificationResult.Reason.NONE
                )
        );
    }
}
