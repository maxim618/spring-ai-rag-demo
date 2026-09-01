package max.example.spring_ai_rag_demo.checklist;

import java.util.List;

public record ChecklistQuestion(
        String questionId,
        String type,
        String question,
        List<ChecklistOption> options,
        List<String> correctOptions
) {
}
