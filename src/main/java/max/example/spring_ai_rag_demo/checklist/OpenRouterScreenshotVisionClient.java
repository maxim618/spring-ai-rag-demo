package max.example.spring_ai_rag_demo.checklist;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.util.Objects;

@Component
public class OpenRouterScreenshotVisionClient implements ScreenshotVisionClient {

    private static final String EXTRACTION_PROMPT = """
            На изображении может быть несколько вопросов чек-листа.
            Извлеки ТОЛЬКО самый верхний вопрос — первый по вертикальному расположению.

            Верни status=READABLE, если верхний вопрос и относящиеся к нему варианты
            можно надёжно прочитать. Верни status=UNREADABLE, если верхний вопрос
            или его варианты обрезаны, неразборчивы либо их нельзя уверенно отличить
            от текста других вопросов.

            Извлекай только варианты ответа верхнего вопроса. Не извлекай вопросы ниже.
            Не переходи к следующему вопросу, даже если он читается лучше.
            Сохраняй исходный смысл и отрицания: «не», «нельзя», «запрещено»,
            «не допускается». Не определяй правильные ответы и не анализируй отметки.
            Не восстанавливай текст по догадке.

            Верни структурированный результат со следующими полями:
            - status: READABLE или UNREADABLE
            - text: текст верхнего вопроса, иначе пустая строка
            - answerOptions: варианты только верхнего вопроса, иначе пустой список
            - reason: краткая причина, если status=UNREADABLE, иначе пустая строка
            """;

    private final ChatClient chatClient;
    private final String model;

    public OpenRouterScreenshotVisionClient(
            ChatClient.Builder chatClientBuilder,
            @org.springframework.beans.factory.annotation.Value(
                    "${knowledge.screenshot.model:nvidia/nemotron-3.5-super-vl:free}"
            ) String model
    ) {
        this.chatClient = Objects.requireNonNull(chatClientBuilder, "chatClientBuilder").build();
        this.model = Objects.requireNonNull(model, "model");
    }

    @Override
    public ScreenshotQuestionExtractionResponse extractTopmostQuestion(
            byte[] image,
            MimeType mimeType
    ) {
        try {
            return chatClient.prompt()
                    .options(OpenAiChatOptions.builder().model(model).build())
                    .user(user -> user
                            .text(EXTRACTION_PROMPT)
                            .media(mimeType, new ByteArrayResource(image)))
                    .call()
                    .entity(ScreenshotQuestionExtractionResponse.class);
        } catch (RuntimeException exception) {
            throw new ScreenshotQuestionExtractionException(
                    "Vision model request failed", exception
            );
        }
    }
}
