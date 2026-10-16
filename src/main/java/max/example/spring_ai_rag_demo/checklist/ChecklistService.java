package max.example.spring_ai_rag_demo.checklist;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

@Service
public class ChecklistService {

    private final ChecklistKnowledgeBaseRetriever retriever;
    private final ChecklistQuestionIdentifier identifier;
    private final Map<String, ChecklistQuestion> questionsById;

    public ChecklistService(
            ChecklistKnowledgeBaseRetriever retriever,
            ChecklistQuestionIdentifier identifier,
            ChecklistKnowledgeBaseLoader knowledgeBaseLoader
    ) {
        this.retriever = Objects.requireNonNull(retriever, "retriever");
        this.identifier = Objects.requireNonNull(identifier, "identifier");
        ChecklistKnowledgeBase knowledgeBase = Objects.requireNonNull(
                knowledgeBaseLoader, "knowledgeBaseLoader"
        ).load();

        List<ChecklistQuestion> questions = List.copyOf(
                Objects.requireNonNull(knowledgeBase.questions(), "knowledgeBase.questions")
        );

        Map<String, ChecklistQuestion> indexedQuestions = new HashMap<>();
        for (ChecklistQuestion question : questions) {
            Objects.requireNonNull(question, "knowledge base question must not be null");
            if (question.questionId() == null || question.questionId().isBlank()) {
                throw new IllegalArgumentException(
                        "Knowledge base question must have a non-blank questionId"
                );
            }
            if (indexedQuestions.putIfAbsent(question.questionId(), question) != null) {
                throw new IllegalArgumentException(
                        "Knowledge base question IDs must be unique: " + question.questionId()
                );
            }
        }
        this.questionsById = Map.copyOf(indexedQuestions);
    }

    public ChecklistQuestionLookupResult identifyQuestion(
            NormalizedChecklistQuestion question
    ) {
        Objects.requireNonNull(question, "question");

        List<org.springframework.ai.document.Document> candidates =
                retriever.retrieve(question.text());

        QuestionIdentificationResult identification =
                identifier.identify(question, candidates);

        if (identification instanceof QuestionIdentificationResult.Identified identified) {
            ChecklistQuestion sourceQuestion = questionsById.get(identified.questionId());

            if (sourceQuestion == null) {
                return new ChecklistQuestionLookupResult.NotIdentified(
                        identified.candidateQuestionIds(),
                        QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                        OptionalDouble.empty()
                );
            }

            return new ChecklistQuestionLookupResult.Identified(
                    sourceQuestion.questionId(),
                    sourceQuestion.type(),
                    sourceQuestion.question(),
                    sourceQuestion.options(),
                    identified.confidence()
            );
        }

        if (identification instanceof QuestionIdentificationResult.Ambiguous ambiguous) {
            return new ChecklistQuestionLookupResult.Ambiguous(
                    ambiguous.candidateQuestionIds(),
                    ambiguous.reason(),
                    ambiguous.confidence()
            );
        }

        QuestionIdentificationResult.NotIdentified notIdentified =
                (QuestionIdentificationResult.NotIdentified) identification;

        return new ChecklistQuestionLookupResult.NotIdentified(
                notIdentified.candidateQuestionIds(),
                notIdentified.reason(),
                notIdentified.confidence()
        );
    }
}
