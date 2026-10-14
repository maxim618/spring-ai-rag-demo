
package max.example.spring_ai_rag_demo.checklist;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

class QuestionIdentificationResultTest {

    @Test
    void shouldCreateIdentifiedResult() {
        var result = new QuestionIdentificationResult.Identified(
                "Q-001",
                List.of("Q-001", "Q-002"),
                OptionalDouble.of(0.95)
        );

        assertEquals("Q-001", result.questionId());
        assertEquals(List.of("Q-001", "Q-002"),
                result.candidateQuestionIds());
        assertEquals(0.95, result.confidence().orElseThrow());
    }

    @Test
    void shouldCreateAmbiguousResult() {
        var result = new QuestionIdentificationResult.Ambiguous(
                List.of("Q-001", "Q-002"),
                QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES,
                OptionalDouble.empty()
        );

        assertEquals(
                List.of("Q-001", "Q-002"),
                result.candidateQuestionIds()
        );
        assertEquals(
                QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES,
                result.reason()
        );
        assertTrue(result.confidence().isEmpty());
    }

    @Test
    void shouldCreateNotIdentifiedResult() {
        var result = new QuestionIdentificationResult.NotIdentified(
                List.of("Q-001"),
                QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                OptionalDouble.empty()
        );

        assertEquals(List.of("Q-001"), result.candidateQuestionIds());
        assertEquals(
                QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                result.reason()
        );
        assertTrue(result.confidence().isEmpty());
    }

    @Test
    void shouldRejectIdentifiedQuestionNotPresentAmongCandidates() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult.Identified(
                        "Q-003",
                        List.of("Q-001", "Q-002"),
                        OptionalDouble.of(0.9)
                )
        );
    }

    @Test
    void shouldRejectBlankQuestionId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult.Identified(
                        " ",
                        List.of("Q-001"),
                        OptionalDouble.empty()
                )
        );
    }

    @Test
    void shouldRejectAmbiguousResultWithOneCandidate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult.Ambiguous(
                        List.of("Q-001"),
                        QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES,
                        OptionalDouble.empty()
                )
        );
    }

    @Test
    void shouldRejectAmbiguousResultWithoutCandidates() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult.Ambiguous(
                        List.of(),
                        QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES,
                        OptionalDouble.empty()
                )
        );
    }

    @Test
    void shouldRejectNotIdentifiedResultWithMultipleCandidatesReason() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult.NotIdentified(
                        List.of("Q-001", "Q-002"),
                        QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES,
                        OptionalDouble.empty()
                )
        );
    }

    @Test
    void shouldRejectNullConfidence() {
        assertThrows(
                NullPointerException.class,
                () -> new QuestionIdentificationResult.Identified(
                        "Q-001",
                        List.of("Q-001"),
                        null
                )
        );
    }

    @Test
    void shouldRejectConfidenceBelowZero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult.Identified(
                        "Q-001",
                        List.of("Q-001"),
                        OptionalDouble.of(-0.1)
                )
        );
    }

    @Test
    void shouldRejectConfidenceAboveOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult.Identified(
                        "Q-001",
                        List.of("Q-001"),
                        OptionalDouble.of(1.1)
                )
        );
    }

    @Test
    void shouldRejectNaNConfidence() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult.Identified(
                        "Q-001",
                        List.of("Q-001"),
                        OptionalDouble.of(Double.NaN)
                )
        );
    }

    @Test
    void shouldRejectDuplicateCandidateIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestionIdentificationResult.Identified(
                        "Q-001",
                        List.of("Q-001", "Q-001"),
                        OptionalDouble.empty()
                )
        );
    }

    @Test
    void shouldRejectNullCandidateId() {
        assertThrows(
                NullPointerException.class,
                () -> new QuestionIdentificationResult.Identified(
                        "Q-001",
                        java.util.Arrays.asList("Q-001", null),
                        OptionalDouble.empty()
                )
        );
    }

    @Test
    void shouldMakeCandidateIdsImmutable() {
        var source = new java.util.ArrayList<>(
                List.of("Q-001", "Q-002")
        );

        var result = new QuestionIdentificationResult.Identified(
                "Q-001",
                source,
                OptionalDouble.empty()
        );

        source.add("Q-003");

        assertEquals(
                List.of("Q-001", "Q-002"),
                result.candidateQuestionIds()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.candidateQuestionIds().add("Q-004")
        );
    }
}
