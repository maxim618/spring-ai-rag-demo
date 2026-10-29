package max.example.spring_ai_rag_demo.checklist;

import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.List;
import java.util.Objects;

@Service
public class ScreenshotQuestionExtractor {

    private static final List<String> SUPPORTED_IMAGE_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final ScreenshotVisionClient visionClient;

    public ScreenshotQuestionExtractor(ScreenshotVisionClient visionClient) {
        this.visionClient = Objects.requireNonNull(visionClient, "visionClient");
    }

    public ExtractedScreenshotQuestion extractTopmostQuestion(
            byte[] image,
            String contentType
    ) {
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("Screenshot must not be empty");
        }

        MimeType mimeType = parseAndValidateContentType(contentType);
        ScreenshotQuestionExtractionResponse response;
        try {
            response = visionClient.extractTopmostQuestion(image.clone(), mimeType);
        } catch (ScreenshotQuestionExtractionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ScreenshotQuestionExtractionException(
                    "Screenshot question recognition failed", exception
            );
        }

        if (response == null || response.status() == null) {
            throw new ScreenshotQuestionExtractionException(
                    "Vision model returned an invalid extraction response"
            );
        }

        if (response.status() == ScreenshotQuestionExtractionResponse.Status.UNREADABLE) {
            String reason = response.reason() == null || response.reason().isBlank()
                    ? "The topmost question or its answer options could not be read reliably"
                    : response.reason().strip();
            throw new ScreenshotQuestionExtractionException(reason);
        }

        String text = response.text() == null ? "" : response.text().strip();
        if (text.isBlank()) {
            throw new ScreenshotQuestionExtractionException(
                    "Vision model marked the topmost question readable but returned no question text"
            );
        }

        List<String> answerOptions = response.answerOptions() == null
                ? List.of()
                : response.answerOptions().stream()
                        .filter(Objects::nonNull)
                        .map(String::strip)
                        .filter(option -> !option.isBlank())
                        .toList();

        return new ExtractedScreenshotQuestion(text, answerOptions);
    }

    private MimeType parseAndValidateContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Screenshot content type is required");
        }

        MimeType mimeType;
        try {
            mimeType = MimeTypeUtils.parseMimeType(contentType);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported screenshot content type", exception);
        }

        if (!SUPPORTED_IMAGE_TYPES.contains(mimeType.toString().toLowerCase())) {
            throw new IllegalArgumentException(
                    "Screenshot must be JPEG, PNG, or WebP"
            );
        }

        return mimeType;
    }

    public record ExtractedScreenshotQuestion(
            String text,
            List<String> answerOptions
    ) {
        public ExtractedScreenshotQuestion {
            text = Objects.requireNonNull(text, "text").strip();
            answerOptions = List.copyOf(
                    Objects.requireNonNull(answerOptions, "answerOptions")
            );
        }
    }
}
