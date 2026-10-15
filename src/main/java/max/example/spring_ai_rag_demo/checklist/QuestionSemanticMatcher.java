
package max.example.spring_ai_rag_demo.checklist;

import org.springframework.ai.document.Document;

import java.util.List;

public interface QuestionSemanticMatcher {

    QuestionIdentificationResult match(
            NormalizedChecklistQuestion question,
            List<Document> candidates
    );
}
