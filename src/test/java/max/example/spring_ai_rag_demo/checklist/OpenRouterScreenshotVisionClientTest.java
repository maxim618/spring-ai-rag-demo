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
                // 1. Границы текста задания
                .contains("Задание может состоять из одного или нескольких предложений")
                .contains("заканчиваться точкой, вопросительным знаком, двоеточием")
                .contains("Не используй пунктуацию как единственный признак границ задания")

// 2. Структура карточек и вариантов ответа
                .contains("Внутри каждой карточки слева находится текст варианта")
                .contains("справа — квадратный чекбокс")
                .contains("Блоки без чекбокса")
                .contains("вариантами ответа не являются")

// 3. Поиск задания и сбор вариантов
                .contains("Найди первый самостоятельный текст задания на сером фоне")
                .contains("Собери белые карточки с чекбоксами")
                .contains("Сохраняй их исходный порядок")
                .contains("Извлекай только первое найденное задание и его варианты")

// 4. Ограничения распознавания
                .contains("Не угадывай его содержимое")
                .contains("Не дополняй отсутствующие фрагменты по смыслу")
                .contains("status\": \"UNREADABLE\"")

// 5. Определение статуса
                .contains("Используй строго два статуса: READABLE и UNREADABLE")
                .contains("Статус определяется исключительно читаемостью текста первого выбранного задания")

// 6. Запрет на дополнительные действия
                .contains("Не анализируй, какие чекбоксы нажаты")
                .contains("Не определяй правильность вариантов")
                .contains("Не сопоставляй вопрос с базой знаний и не выбирай его идентификатор");
    }
}
