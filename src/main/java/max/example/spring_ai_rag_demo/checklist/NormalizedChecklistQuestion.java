package max.example.spring_ai_rag_demo.checklist;

import java.util.List;
import java.util.Objects;

public record NormalizedChecklistQuestion(
        String text,
        List<String> answerOptions,
        List<String> selectedOptions
) {

    public NormalizedChecklistQuestion {
        text = Objects.requireNonNull(
                text, "text must not be null"
        ).strip();

        if (text.isBlank()) {
            throw new IllegalArgumentException(
                    "text must not be blank"
            );
        }

        answerOptions = List.copyOf(
                Objects.requireNonNull(
                        answerOptions,
                        "answerOptions must not be null"
                )
        );

        selectedOptions = List.copyOf(
                Objects.requireNonNull(
                        selectedOptions,
                        "selectedOptions must not be null"
                )
        );

        if (answerOptions.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException(
                    "Answer options must not be blank"
            );
        }

        if (selectedOptions.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException(
                    "Selected options must not be blank"
            );
        }
    }

    public static NormalizedChecklistQuestion textOnly(String text) {
        return new NormalizedChecklistQuestion(
                text,
                List.of(),
                List.of()
        );
    }
}
