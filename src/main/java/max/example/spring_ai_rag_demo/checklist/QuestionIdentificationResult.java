
package max.example.spring_ai_rag_demo.checklist;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;

public sealed interface QuestionIdentificationResult
        permits QuestionIdentificationResult.Identified,
        QuestionIdentificationResult.Ambiguous,
        QuestionIdentificationResult.NotIdentified {

    List<String> candidateQuestionIds();

    OptionalDouble confidence();

    enum Reason {
        MULTIPLE_CANDIDATES,
        NO_RELIABLE_MATCH,
        NEGATION_MISMATCH,
        MULTIPLE_TOPICS
    }

    record Identified(
            String questionId,
            List<String> candidateQuestionIds,
            OptionalDouble confidence
    ) implements QuestionIdentificationResult {

        public Identified {
            questionId = requireNonBlank(questionId, "questionId");
            candidateQuestionIds =
                    validatedCandidateIds(candidateQuestionIds);
            confidence = validatedConfidence(confidence);

            if (!candidateQuestionIds.contains(questionId)) {
                throw new IllegalArgumentException(
                        "Identified question must be among candidates"
                );
            }
        }
    }

    record Ambiguous(
            List<String> candidateQuestionIds,
            Reason reason,
            OptionalDouble confidence
    ) implements QuestionIdentificationResult {

        public Ambiguous {
            candidateQuestionIds =
                    validatedCandidateIds(candidateQuestionIds);
            reason = Objects.requireNonNull(reason, "reason");
            confidence = validatedConfidence(confidence);

            if (candidateQuestionIds.size() < 2) {
                throw new IllegalArgumentException(
                        "Ambiguous result requires at least two candidates"
                );
            }

            if (reason == Reason.NO_RELIABLE_MATCH) {
                throw new IllegalArgumentException(
                        "Ambiguous result cannot use NO_RELIABLE_MATCH"
                );
            }
        }
    }

    record NotIdentified(
            List<String> candidateQuestionIds,
            Reason reason,
            OptionalDouble confidence
    ) implements QuestionIdentificationResult {

        public NotIdentified {
            candidateQuestionIds =
                    validatedCandidateIds(candidateQuestionIds);
            reason = Objects.requireNonNull(reason, "reason");
            confidence = validatedConfidence(confidence);

            if (reason == Reason.MULTIPLE_CANDIDATES) {
                throw new IllegalArgumentException(
                        "NotIdentified cannot use MULTIPLE_CANDIDATES"
                );
            }
        }
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " must not be blank"
            );
        }

        return value;
    }

    private static List<String> validatedCandidateIds(
            List<String> candidateQuestionIds
    ) {
        Objects.requireNonNull(
                candidateQuestionIds, "candidateQuestionIds"
        );

        List<String> ids = List.copyOf(candidateQuestionIds);
        Set<String> uniqueIds = new HashSet<>();

        for (String id : ids) {
            requireNonBlank(id, "candidate question ID");

            if (!uniqueIds.add(id)) {
                throw new IllegalArgumentException(
                        "Candidate question IDs must be unique"
                );
            }
        }

        return ids;
    }

    private static OptionalDouble validatedConfidence(
            OptionalDouble confidence
    ) {
        Objects.requireNonNull(confidence, "confidence");

        if (confidence.isPresent()) {
            double value = confidence.getAsDouble();

            if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException(
                        "Confidence must be finite and between 0 and 1"
                );
            }
        }

        return confidence;
    }
}
