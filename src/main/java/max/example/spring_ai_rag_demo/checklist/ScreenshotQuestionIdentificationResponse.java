package max.example.spring_ai_rag_demo.checklist;

import java.util.List;

public record ScreenshotQuestionIdentificationResponse(
        String extractedQuestion,
        List<String> extractedAnswerOptions,
        ChecklistQuestionLookupResult identification
) {
    public ScreenshotQuestionIdentificationResponse {
        extractedAnswerOptions = List.copyOf(extractedAnswerOptions);
    }
}
