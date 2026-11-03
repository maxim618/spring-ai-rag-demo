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
                    Найди первый вопрос сверху вниз, у которого на изображении виден текст самого задания.
            
                    Не определяй вопрос только по грамматической форме или знакам препинания.
            
                    Формулировка задания может:
                    - заканчиваться вопросительным знаком;
                    - заканчиваться двоеточием перед перечнем вариантов;
                    - быть повелительным предложением («Укажите...», «Выберите...»,
                      «Определите...», «Отметьте...»);
                    - быть утверждением, после которого требуется выбрать один или несколько ответов.
            
                    Не требуй наличия чекбокса или другого элемента выбора непосредственно
                    рядом с текстом задания. Чекбоксы могут находиться только возле вариантов ответа.
            
                    Определяй карточку по совокупности признаков:
                    - текст задания или инструкции;
                    - расположенные ниже варианты ответа;
                    - визуальная группировка текста и вариантов в одну карточку.
            
                    Важно отличать вопрос от продолжения предыдущей карточки:
                    - Фрагмент, содержащий только варианты ответа, чекбоксы или отметки без текста
                      задания, не считай отдельным вопросом. Это может быть нижняя часть предыдущей карточки.
                    - Если такой фрагмент расположен над следующей карточкой с видимым текстом задания,
                      извлеки следующую карточку.
                    - Отдельный вариант ответа с чекбоксом не является формулировкой задания.
            
                    После определения первой карточки с видимым текстом задания не переходи к карточкам ниже.
                    Если текст этого задания обрезан, неразборчив или его нельзя надёжно восстановить,
                    верни status=UNREADABLE. Не пропускай его ради более читаемого задания ниже.
            
                    Верни status=READABLE, если текст выбранного задания и относящиеся к нему варианты
                    можно надёжно прочитать. Верни status=UNREADABLE, если текст выбранного задания
                    или необходимые для его распознавания варианты обрезаны, неразборчивы либо их
                    нельзя уверенно отличить от текста других карточек.
            
                    Извлекай только варианты ответа выбранного вопроса. Не извлекай варианты из
                    предыдущих или последующих карточек. Сохраняй исходный смысл и отрицания:
                    «не», «нельзя», «запрещено», «не допускается». Не определяй правильные ответы
                    и не анализируй отметки. Не восстанавливай текст по догадке.
            
                    Верни структурированный результат со следующими полями:
                    - status: READABLE или UNREADABLE
                    - text: текст выбранного задания, иначе пустая строка
                    - answerOptions: варианты только выбранного задания, иначе пустой список
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
                    .options(OpenAiChatOptions.builder().model(model))
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
