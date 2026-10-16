package max.example.spring_ai_rag_demo.checklist;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

public sealed interface ChecklistQuestionLookupResult
        permits ChecklistQuestionLookupResult.Identified,
        ChecklistQuestionLookupResult.Ambiguous,
        ChecklistQuestionLookupResult.NotIdentified {

    record Identified(
            String questionId,
            String type,
            String question,
            List<ChecklistOption> options,
            OptionalDouble confidence
    ) implements ChecklistQuestionLookupResult {

        public Identified {
            questionId = requireNonBlank(questionId, "questionId");
            type = requireNonBlank(type, "type");
            question = requireNonBlank(question, "question");
            options = List.copyOf(Objects.requireNonNull(options, "options"));
            confidence = Objects.requireNonNull(confidence, "confidence");
        }
    }

    record Ambiguous(
            List<String> candidateQuestionIds,
            QuestionIdentificationResult.Reason reason,
            OptionalDouble confidence
    ) implements ChecklistQuestionLookupResult {

        public Ambiguous {
            candidateQuestionIds = List.copyOf(
                    Objects.requireNonNull(candidateQuestionIds, "candidateQuestionIds")
            );
            reason = Objects.requireNonNull(reason, "reason");
            confidence = Objects.requireNonNull(confidence, "confidence");
        }
    }

    record NotIdentified(
            List<String> candidateQuestionIds,
            QuestionIdentificationResult.Reason reason,
            OptionalDouble confidence
    ) implements ChecklistQuestionLookupResult {

        public NotIdentified {
            candidateQuestionIds = List.copyOf(
                    Objects.requireNonNull(candidateQuestionIds, "candidateQuestionIds")
            );
            reason = Objects.requireNonNull(reason, "reason");
            confidence = Objects.requireNonNull(confidence, "confidence");
        }
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
