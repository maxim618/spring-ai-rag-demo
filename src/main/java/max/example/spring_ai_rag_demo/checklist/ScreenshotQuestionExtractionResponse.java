package max.example.spring_ai_rag_demo.checklist;

import java.util.List;

/**
 * Structured response returned by the vision model for a screenshot.
 * Only the topmost question is eligible for extraction.
 */
public record ScreenshotQuestionExtractionResponse(
        Status status,
        String text,
        List<String> answerOptions,
        String reason
) {
    public enum Status {
        READABLE,
        UNREADABLE
    }
}
