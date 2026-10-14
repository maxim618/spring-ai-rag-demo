
package max.example.spring_ai_rag_demo.checklist;

import java.util.List;
import java.util.Objects;

public record QuestionIdentificationResult(
        Status status,
        String questionId,
        List<String> candidateQuestionIds,
        Double confidence,
        Reason reason
) {

    public enum Status {
        IDENTIFIED,
        AMBIGUOUS,
        NOT_IDENTIFIED
    }

    public enum Reason {
        NONE,
        MULTIPLE_CANDIDATES,
        NO_RELIABLE_MATCH,
        NEGATION_MISMATCH,
        MULTIPLE_TOPICS
    }

    public QuestionIdentificationResult {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(reason, "reason must not be null");

        candidateQuestionIds = List.copyOf(
                Objects.requireNonNull(
                        candidateQuestionIds,
                        "candidateQuestionIds must not be null"
                )
        );

        if (candidateQuestionIds.stream()
                .anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException(
                    "Candidate question IDs must not be blank"
            );
        }

        if (candidateQuestionIds.stream().distinct().count()
                != candidateQuestionIds.size()) {
            throw new IllegalArgumentException(
                    "Candidate question IDs must be unique"
            );
        }

        if (confidence != null
                && (!Double.isFinite(confidence)
                || confidence < 0.0
                || confidence > 1.0)) {
            throw new IllegalArgumentException(
                    "Confidence must be between 0.0 and 1.0"
            );
        }

        switch (status) {
            case IDENTIFIED -> {
                if (questionId == null || questionId.isBlank()) {
                    throw new IllegalArgumentException(
                            "IDENTIFIED requires a questionId"
                    );
                }

                if (reason != Reason.NONE) {
                    throw new IllegalArgumentException(
                            "IDENTIFIED requires reason NONE"
                    );
                }
            }

            case AMBIGUOUS -> {
                if (questionId != null) {
                    throw new IllegalArgumentException(
                            "AMBIGUOUS must not select a questionId"
                    );
                }

                if (candidateQuestionIds.size() < 2) {
                    throw new IllegalArgumentException(
                            "AMBIGUOUS requires at least two candidates"
                    );
                }

                if (reason != Reason.MULTIPLE_CANDIDATES) {
                    throw new IllegalArgumentException(
                            "AMBIGUOUS requires reason MULTIPLE_CANDIDATES"
                    );
                }
            }

            case NOT_IDENTIFIED -> {
                if (questionId != null) {
                    throw new IllegalArgumentException(
                            "NOT_IDENTIFIED must not select a questionId"
                    );
                }

                if (reason == Reason.NONE
                        || reason == Reason.MULTIPLE_CANDIDATES) {
                    throw new IllegalArgumentException(
                            "NOT_IDENTIFIED requires a failure reason"
                    );
                }
            }
        }
    }
}
