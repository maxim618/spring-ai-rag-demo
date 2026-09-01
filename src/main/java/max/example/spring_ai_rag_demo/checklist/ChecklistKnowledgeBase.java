package max.example.spring_ai_rag_demo.checklist;

import java.util.List;

public record ChecklistKnowledgeBase(
        List<ChecklistQuestion> questions
) {
}
