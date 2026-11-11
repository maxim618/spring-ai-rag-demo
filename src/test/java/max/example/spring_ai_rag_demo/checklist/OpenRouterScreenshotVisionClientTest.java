package max.example.spring_ai_rag_demo.checklist;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OpenRouterScreenshotVisionClientTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldSendQuestionCardBoundaryInstructionsToVisionModel() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ScreenshotQuestionExtractionResponse response =
                mock(ScreenshotQuestionExtractionResponse.class);

        ChatClient.ChatClientRequestSpec requestSpec =
                mock(ChatClient.ChatClientRequestSpec.class);

        ChatClient.CallResponseSpec responseSpec =
                mock(ChatClient.CallResponseSpec.class);

        ChatClient.PromptUserSpec userSpec =
                mock(ChatClient.PromptUserSpec.class);

        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.options(any(ChatOptions.Builder.class)))
                .thenReturn(requestSpec);
        when(requestSpec.user(any(Consumer.class)))
                .thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        when(responseSpec.entity(ScreenshotQuestionExtractionResponse.class))
                .thenReturn(response);
        OpenRouterScreenshotVisionClient client =
                new OpenRouterScreenshotVisionClient(builder, "test-model");

        byte[] image = {1, 2, 3};
        MimeType mimeType = MimeType.valueOf("image/jpeg");

        when(userSpec.text(anyString())).thenReturn(userSpec);

        when(userSpec.media(any(MimeType.class), any(Resource.class)))
                .thenReturn(userSpec);

        client.extractTopmostQuestion(image, mimeType);

        var consumerCaptor =
                org.mockito.ArgumentCaptor.forClass(Consumer.class);

        verify(requestSpec).user(consumerCaptor.capture());

        Consumer<ChatClient.PromptUserSpec> userConsumer =
                consumerCaptor.getValue();

        userConsumer.accept(userSpec);

        var textCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);

        verify(userSpec).text(textCaptor.capture());

        String actualPrompt = textCaptor.getValue();

        assertThat(actualPrompt)
                .contains("заканчиваться двоеточием")
                .contains("Не требуй наличия чекбокса")
                .contains("но не виден текст задания")
                .contains("status=UNREADABLE")
                .contains("Не восстанавливай текст по догадке")
                .contains("Работай строго в два этапа")
                .contains("анализируя только изображение")
                .contains("Не используй совпадение отдельного варианта ответа с базой знаний")
                .contains("верхняя часть изображения может начинаться с середины предыдущей карточки")
                .contains("полностью игнорируй этот фрагмент при выборе вопроса")
                .contains("Это правило имеет приоритет над")
                .contains("Не пытайся восстановить задание предыдущей карточки по её вариантам ответа");
    }
}
