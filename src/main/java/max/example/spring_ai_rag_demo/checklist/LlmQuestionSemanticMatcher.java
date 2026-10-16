
package max.example.spring_ai_rag_demo.checklist;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

@Component
public final class LlmQuestionSemanticMatcher implements QuestionSemanticMatcher {

    private final ChatClient chatClient;

    public LlmQuestionSemanticMatcher(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public QuestionIdentificationResult match(
            NormalizedChecklistQuestion question,
            List<Document> candidates
    ) {
        Objects.requireNonNull(question, "question must not be null");
        Objects.requireNonNull(candidates, "candidates must not be null");

        List<Candidate> availableCandidates = candidates.stream()
                .map(LlmQuestionSemanticMatcher::toCandidate)
                .toList();

        if (availableCandidates.isEmpty()) {
            return notIdentified(
                    List.of(),
                    QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH
            );
        }

        MatchDecision decision = chatClient.prompt()
                .system("""
                        You identify checklist questions by meaning.

                        Compare the input question with the supplied candidates.
                        Consider the question text, answer options, selected options,
                        and meaning-critical negation.

                        Rules:
                        - Select only an ID from the supplied candidates.
                        - Do not invent or modify IDs.
                        - Preserve the meaning of negation. Opposite meanings are
                          not equivalent.
                        - If multiple candidates fit and none can be selected
                          reliably, return AMBIGUOUS.
                        - If no candidate fits reliably, return NOT_IDENTIFIED.
                        - Do not answer the checklist question itself.
                        - Return a structured response matching the requested schema.
                        """)
                .user(userPrompt(question, availableCandidates))
                .call()
                .entity(MatchDecision.class);

        if (decision == null || decision.status() == null) {
            return notIdentified(
                    idsOf(availableCandidates),
                    QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH
            );
        }

        return toResult(decision, availableCandidates);
    }

    private static String userPrompt(
            NormalizedChecklistQuestion question,
            List<Candidate> candidates
    ) {
        return """
                Input question:
                %s

                Answer options:
                %s

                Selected options:
                %s

                Candidate questions:
                %s

                Choose one status:
                IDENTIFIED, AMBIGUOUS, or NOT_IDENTIFIED.

                For IDENTIFIED, provide questionId.
                For AMBIGUOUS, provide at least two candidateQuestionIds.
                For NOT_IDENTIFIED, provide a reason.
                Confidence is an estimate from 0.0 to 1.0, not a calibrated
                probability. Use 0.0 if you cannot estimate it.
                """.formatted(
                question.text(),
                question.answerOptions(),
                question.selectedOptions(),
                candidates
        );
    }

    private static Candidate toCandidate(Document document) {
        Objects.requireNonNull(document, "candidate document must not be null");

        Object value = document.getMetadata().get("questionId");

        if (!(value instanceof String questionId) || questionId.isBlank()) {
            throw new IllegalArgumentException(
                    "Candidate document must have a non-blank questionId metadata"
            );
        }

        return new Candidate(questionId, document.getText());
    }

    private static QuestionIdentificationResult toResult(
            MatchDecision decision,
            List<Candidate> candidates
    ) {
        List<String> availableIds = idsOf(candidates);

        if (!validConfidence(decision.confidence())) {
            return notIdentified(
                    availableIds,
                    QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH
            );
        }

        OptionalDouble confidence = OptionalDouble.of(decision.confidence());

        List<String> selectedIds = decision.candidateQuestionIds() == null
                ? List.of()
                : List.copyOf(decision.candidateQuestionIds());

        if (selectedIds.stream().anyMatch(id -> !availableIds.contains(id))
                || selectedIds.stream().distinct().count() != selectedIds.size()) {
            return notIdentified(
                    availableIds,
                    QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH
            );
        }

        return switch (decision.status()) {
            case IDENTIFIED -> identified(
                    decision.questionId(),
                    selectedIds,
                    availableIds,
                    confidence
            );
            case AMBIGUOUS -> ambiguous(
                    selectedIds,
                    availableIds,
                    decision.reason(),
                    confidence
            );
            case NOT_IDENTIFIED -> notIdentified(
                    availableIds,
                    mapReason(decision.reason())
            );
        };
    }

    private static QuestionIdentificationResult identified(
            String questionId,
            List<String> selectedIds,
            List<String> availableIds,
            OptionalDouble confidence
    ) {
        if (questionId == null
                || !availableIds.contains(questionId)
                || (!selectedIds.isEmpty() && !selectedIds.contains(questionId))) {
            return notIdentified(
                    availableIds,
                    QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH
            );
        }

        return new QuestionIdentificationResult.Identified(
                questionId,
                availableIds,
                confidence
        );
    }

    private static QuestionIdentificationResult ambiguous(
            List<String> selectedIds,
            List<String> availableIds,
            String reason,
            OptionalDouble confidence
    ) {
        if (selectedIds.size() < 2) {
            return notIdentified(
                    availableIds,
                    QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH
            );
        }

        QuestionIdentificationResult.Reason mappedReason = mapReason(reason);

        if (mappedReason == QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH) {
            mappedReason = QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES;
        }

        return new QuestionIdentificationResult.Ambiguous(
                selectedIds,
                mappedReason,
                confidence
        );
    }

    private static QuestionIdentificationResult.NotIdentified notIdentified(
            List<String> ids,
            QuestionIdentificationResult.Reason reason
    ) {
        return new QuestionIdentificationResult.NotIdentified(
                ids,
                reason,
                OptionalDouble.empty()
        );
    }

    private static QuestionIdentificationResult.Reason mapReason(String reason) {
        if (reason == null) {
            return QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH;
        }

        try {
            return QuestionIdentificationResult.Reason.valueOf(reason);
        } catch (IllegalArgumentException exception) {
            return QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH;
        }
    }

    private static boolean validConfidence(Double confidence) {
        return confidence != null
                && Double.isFinite(confidence)
                && confidence >= 0.0
                && confidence <= 1.0;
    }

    private static List<String> idsOf(List<Candidate> candidates) {
        return candidates.stream()
                .map(Candidate::questionId)
                .toList();
    }

    private record Candidate(String questionId, String text) {
    }

    enum Status {
        IDENTIFIED,
        AMBIGUOUS,
        NOT_IDENTIFIED
    }

    public record MatchDecision(
            Status status,
            String questionId,
            List<String> candidateQuestionIds,
            String reason,
            Double confidence
    ) {
    }
}
