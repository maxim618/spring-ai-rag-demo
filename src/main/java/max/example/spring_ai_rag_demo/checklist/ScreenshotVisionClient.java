package max.example.spring_ai_rag_demo.checklist;

import org.springframework.util.MimeType;

/**
 * Boundary around the external vision model so extraction rules can be tested
 * without making network calls.
 */
public interface ScreenshotVisionClient {

    ScreenshotQuestionExtractionResponse extractTopmostQuestion(
            byte[] image,
            MimeType mimeType
    );
}
