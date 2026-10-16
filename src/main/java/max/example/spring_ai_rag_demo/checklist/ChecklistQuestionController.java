package max.example.spring_ai_rag_demo.checklist;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/checklist/questions")
public class ChecklistQuestionController {

    private final ChecklistService checklistService;

    public ChecklistQuestionController(ChecklistService checklistService) {
        this.checklistService = checklistService;
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableRequest(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity.badRequest().body(
                Map.of("error", "Request body must be valid JSON and contain a question")
        );
    }
}
