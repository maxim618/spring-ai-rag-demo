package max.example.spring_ai_rag_demo.checklist;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/checklist/questions")
public class ChecklistQuestionController {

    private final ChecklistService checklistService;
    private final ScreenshotQuestionExtractor screenshotQuestionExtractor;

    public ChecklistQuestionController(
            ChecklistService checklistService,
            ScreenshotQuestionExtractor screenshotQuestionExtractor
    ) {
        this.checklistService = checklistService;
        this.screenshotQuestionExtractor = screenshotQuestionExtractor;
    }

    @PostMapping("/identify")
    public ResponseEntity<?> identify(@RequestBody ChecklistQuestionIdentificationRequest request) {
        final NormalizedChecklistQuestion normalizedQuestion;
        try {
            normalizedQuestion = request.toNormalizedQuestion();
        } catch (IllegalArgumentException | NullPointerException exception) {
            String message = exception.getMessage() == null
                    ? "Request must contain a valid question"
                    : exception.getMessage();
            return ResponseEntity.badRequest().body(Map.of("error", message));
        }

        ChecklistQuestionLookupResult result = checklistService.identifyQuestion(normalizedQuestion);

        if (result instanceof ChecklistQuestionLookupResult.Identified) {
            return ResponseEntity.ok(result);
        }
        if (result instanceof ChecklistQuestionLookupResult.Ambiguous) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        return ResponseEntity.unprocessableEntity().body(result);
    }

    @PostMapping(value = "/identify-screenshot", consumes = "multipart/form-data")
    public ResponseEntity<?> identifyScreenshot(
            @RequestPart("image") MultipartFile image
    ) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Screenshot must not be empty"));
        }

        final ScreenshotQuestionExtractor.ExtractedScreenshotQuestion extracted;
        try {
            extracted = screenshotQuestionExtractor.extractTopmostQuestion(
                    image.getBytes(),
                    image.getContentType()
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        } catch (ScreenshotQuestionExtractionException exception) {
            return ResponseEntity.unprocessableEntity().body(
                    Map.of("error", exception.getMessage())
            );
        } catch (IOException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Could not read uploaded screenshot")
            );
        }

        NormalizedChecklistQuestion question = new NormalizedChecklistQuestion(
                extracted.text(),
                extracted.answerOptions(),
                List.of()
        );
        ChecklistQuestionLookupResult result = checklistService.identifyQuestion(question);

        ScreenshotQuestionIdentificationResponse response =
                new ScreenshotQuestionIdentificationResponse(
                        extracted.text(),
                        extracted.answerOptions(),
                        result
                );

        if (result instanceof ChecklistQuestionLookupResult.Identified) {
            return ResponseEntity.ok(response);
        }
        if (result instanceof ChecklistQuestionLookupResult.Ambiguous) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.unprocessableEntity().body(response);
    }

    @ExceptionHandler(EmbeddingProviderRateLimitException.class)
    public ResponseEntity<Map<String, String>> handleEmbeddingProviderRateLimit(
            EmbeddingProviderRateLimitException exception
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                Map.of(
                        "error",
                        "Question search is temporarily unavailable because the embedding provider rate limit was reached"
                )
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableRequest(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity.badRequest().body(
                Map.of("error", "Request body must be valid JSON and contain a question")
        );
    }

    @ExceptionHandler(QuestionIdentificationException.class)
    public ResponseEntity<Map<String, String>> handleQuestionIdentificationFailure(
            QuestionIdentificationException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                Map.of(
                        "error",
                        "Question identification service returned an invalid response"
                )
        );
    }
}
