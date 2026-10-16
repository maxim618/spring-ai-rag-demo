package max.example.spring_ai_rag_demo.checklist;

import java.util.List;

public record ChecklistQuestionIdentificationRequest(
        String text,
        List<String> answerOptions,
        List<String> selectedOptions
) {

    public NormalizedChecklistQuestion toNormalizedQuestion() {
        return new NormalizedChecklistQuestion(
                text,
                answerOptions == null ? List.of() : answerOptions,
                selectedOptions == null ? List.of() : selectedOptions
        );
    }
}
