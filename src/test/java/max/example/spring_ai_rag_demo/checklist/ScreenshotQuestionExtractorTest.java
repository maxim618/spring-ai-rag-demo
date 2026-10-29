package max.example.spring_ai_rag_demo.checklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ScreenshotQuestionExtractorTest {

    private ScreenshotVisionClient visionClient;
    private ScreenshotQuestionExtractor extractor;

    @BeforeEach
    void setUp() {
        visionClient = mock(ScreenshotVisionClient.class);
        extractor = new ScreenshotQuestionExtractor(visionClient);
    }

    @Test
    void shouldReturnOnlyExtractedTopmostQuestionAndItsOptions() {
        byte[] image = {1, 2, 3};
        when(visionClient.extractTopmostQuestion(image, MimeTypeUtils.IMAGE_PNG))
                .thenReturn(new ScreenshotQuestionExtractionResponse(
                        ScreenshotQuestionExtractionResponse.Status.READABLE,
                        "  Как проверить заказ?  ",
                        List.of(" Вариант A ", "Вариант B", " "),
                        ""
                ));

        ScreenshotQuestionExtractor.ExtractedScreenshotQuestion result =
                extractor.extractTopmostQuestion(image, "image/png");

        assertThat(result.text()).isEqualTo("Как проверить заказ?");
        assertThat(result.answerOptions()).containsExactly("Вариант A", "Вариант B");
        verify(visionClient).extractTopmostQuestion(image, MimeTypeUtils.IMAGE_PNG);
    }

    @Test
    void shouldFailInsteadOfFallingBackToQuestionBelowWhenTopmostIsUnreadable() {
        when(visionClient.extractTopmostQuestion(imageBytes(), MimeTypeUtils.IMAGE_JPEG))
                .thenReturn(new ScreenshotQuestionExtractionResponse(
                        ScreenshotQuestionExtractionResponse.Status.UNREADABLE,
                        "",
                        List.of(),
                        "Верхний вопрос обрезан"
                ));

        assertThatThrownBy(() -> extractor.extractTopmostQuestion(imageBytes(), "image/jpeg"))
                .isInstanceOf(ScreenshotQuestionExtractionException.class)
                .hasMessage("Верхний вопрос обрезан");
    }

    @Test
    void shouldRejectEmptyImageBeforeCallingVisionModel() {
        assertThatThrownBy(() -> extractor.extractTopmostQuestion(new byte[0], "image/png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Screenshot must not be empty");

        verifyNoInteractions(visionClient);
    }

    @Test
    void shouldRejectUnsupportedContentTypeBeforeCallingVisionModel() {
        assertThatThrownBy(() -> extractor.extractTopmostQuestion(imageBytes(), "text/plain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Screenshot must be JPEG, PNG, or WebP");

        verifyNoInteractions(visionClient);
    }

    @Test
    void shouldRejectReadableResponseWithoutQuestionText() {
        when(visionClient.extractTopmostQuestion(imageBytes(), MimeTypeUtils.IMAGE_JPEG))
                .thenReturn(new ScreenshotQuestionExtractionResponse(
                        ScreenshotQuestionExtractionResponse.Status.READABLE,
                        " ",
                        List.of("Option A"),
                        ""
                ));

        assertThatThrownBy(() -> extractor.extractTopmostQuestion(imageBytes(), "image/jpeg"))
                .isInstanceOf(ScreenshotQuestionExtractionException.class)
                .hasMessageContaining("returned no question text");
    }

    private static byte[] imageBytes() {
        return new byte[]{1, 2, 3};
    }
}
