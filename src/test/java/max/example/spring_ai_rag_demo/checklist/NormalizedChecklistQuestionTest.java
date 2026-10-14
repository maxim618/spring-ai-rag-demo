package max.example.spring_ai_rag_demo.checklist;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NormalizedChecklistQuestionTest {

    @Test
    void shouldCreateTextOnlyQuestion() {
        var question =
                NormalizedChecklistQuestion.textOnly(
                        "  Что проверить?  "
                );

        assertEquals("Что проверить?", question.text());
        assertTrue(question.answerOptions().isEmpty());
        assertTrue(question.selectedOptions().isEmpty());
    }

    @Test
    void shouldPreserveAnswerOptions() {
        var question = new NormalizedChecklistQuestion(
                "Что проверить?",
                List.of("Документы", "Посылку"),
                List.of("Документы")
        );

        assertEquals(
                List.of("Документы", "Посылку"),
                question.answerOptions()
        );
        assertEquals(
                List.of("Документы"),
                question.selectedOptions()
        );
    }

    @Test
    void shouldMakeListsImmutable() {
        var options = new ArrayList<>(
                List.of("Документы")
        );

        var question = new NormalizedChecklistQuestion(
                "Что проверить?",
                options,
                List.of()
        );

        options.add("Посылку");

        assertEquals(
                List.of("Документы"),
                question.answerOptions()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> question.answerOptions().add("Посылку")
        );
    }

    @Test
    void shouldRejectNullText() {
        assertThrows(
                NullPointerException.class,
                () -> NormalizedChecklistQuestion.textOnly(null)
        );
    }

    @Test
    void shouldRejectBlankText() {
        assertThrows(
                IllegalArgumentException.class,
                () -> NormalizedChecklistQuestion.textOnly("   ")
        );
    }

    @Test
    void shouldRejectNullAnswerOptions() {
        assertThrows(
                NullPointerException.class,
                () -> new NormalizedChecklistQuestion(
                        "Что проверить?",
                        null,
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectBlankAnswerOption() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NormalizedChecklistQuestion(
                        "Что проверить?",
                        List.of(" "),
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectNullSelectedOptions() {
        assertThrows(
                NullPointerException.class,
                () -> new NormalizedChecklistQuestion(
                        "Что проверить?",
                        List.of(),
                        null
                )
        );
    }
}
