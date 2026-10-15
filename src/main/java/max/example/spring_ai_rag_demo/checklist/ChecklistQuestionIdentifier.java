package max.example.spring_ai_rag_demo.checklist;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;

public final class ChecklistQuestionIdentifier {

    private final QuestionSemanticMatcher semanticMatcher;

    public ChecklistQuestionIdentifier(
            QuestionSemanticMatcher semanticMatcher
    ) {
        this.semanticMatcher = Objects.requireNonNull(
                semanticMatcher,
                "semanticMatcher must not be null"
        );
    }

    public QuestionIdentificationResult identify(
            NormalizedChecklistQuestion question,
            List<Document> candidates
    ) {
        Objects.requireNonNull(question, "question must not be null");
        Objects.requireNonNull(candidates, "candidates must not be null");

        List<Document> documents = List.copyOf(candidates);
        List<String> candidateIds = validateAndExtractIds(documents);

        if (documents.isEmpty()) {
            return new QuestionIdentificationResult.NotIdentified(
                    List.of(),
                    QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                    OptionalDouble.empty()
            );
        }

        QuestionIdentificationResult result = Objects.requireNonNull(
                semanticMatcher.match(question, documents),
                "semanticMatcher result must not be null"
        );

        if (!containsOnlyRetrievedIds(result, candidateIds)) {
            return new QuestionIdentificationResult.NotIdentified(
                    candidateIds,
                    QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                    OptionalDouble.empty()
            );
        }

        return result;
    }

    private static List<String> validateAndExtractIds(
            List<Document> documents
    ) {
        List<String> ids = new ArrayList<>();
        Set<String> uniqueIds = new HashSet<>();

        for (Document document : documents) {
            Objects.requireNonNull(document, "candidate document must not be null");

            Object questionId = document.getMetadata().get("questionId");

            if (!(questionId instanceof String id) || id.isBlank()) {
                throw new IllegalArgumentException(
                        "Candidate document must have a non-blank questionId metadata"
                );
            }

            if (!uniqueIds.add(id)) {
                throw new IllegalArgumentException(
                        "Candidate document IDs must be unique"
                );
            }

            ids.add(id);
        }

        return List.copyOf(ids);
    }

    private static boolean containsOnlyRetrievedIds(
            QuestionIdentificationResult result,
            List<String> retrievedIds
    ) {
        Set<String> retrievedIdSet = Set.copyOf(retrievedIds);

        if (result instanceof QuestionIdentificationResult.Identified identified) {
            return retrievedIdSet.contains(identified.questionId())
                    && retrievedIdSet.containsAll(
                    identified.candidateQuestionIds()
            );
        }

        if (result instanceof QuestionIdentificationResult.Ambiguous ambiguous) {
            return retrievedIdSet.containsAll(
                    ambiguous.candidateQuestionIds()
            );
        }

        if (result instanceof QuestionIdentificationResult.NotIdentified notIdentified) {
            return retrievedIdSet.containsAll(
                    notIdentified.candidateQuestionIds()
            );
        }

        return false;
    }
}
