package max.example.spring_ai_rag_demo.checklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.util.Objects;

@Component
public class OpenRouterScreenshotVisionClient implements ScreenshotVisionClient {

    private static final Logger logger =
            LoggerFactory.getLogger(OpenRouterScreenshotVisionClient.class);

    private static final String EXTRACTION_PROMPT = """
            На изображении может быть несколько карточек вопросов чек-листа.
            Найди первую сверху вниз карточку, у которой на изображении виден текст самого задания.

            Работай строго в два этапа:
            1. Сначала определи границы карточек и выбери первую карточку с видимым текстом задания,
               анализируя только изображение.
            2. Только после выбора карточки сопоставь её задание и варианты с вопросами базы знаний.

            Не используй совпадение отдельного варианта ответа с базой знаний для выбора карточки.
            Сначала выбери карточку по её видимому заданию, а не по тому, насколько хорошо её
            варианты ответа совпадают с известным вопросом.

            ВАЖНО: верхняя часть изображения может начинаться с середины предыдущей карточки.
            Если у верхнего фрагмента видны только варианты ответа, чекбоксы или отметки,
            но не виден текст задания, полностью игнорируй этот фрагмент при выборе вопроса.
            Не пытайся восстановить задание предыдущей карточки по её вариантам ответа.
            Не считай текст варианта ответа формулировкой задания, даже если он похож на вопрос
            или точно совпадает с одним из вариантов в базе знаний.

            Если ниже такого неполного фрагмента начинается карточка с собственным видимым
            текстом задания, выбери именно эту карточку. Это правило имеет приоритет над
            совпадением верхних вариантов ответа с базой знаний.

            Формулировка задания может:
            - заканчиваться вопросительным знаком;
            - заканчиваться двоеточием перед перечнем вариантов;
            - быть повелительным предложением («Укажите...», «Выберите...»,
              «Определите...», «Отметьте...»);
            - быть утверждением, после которого требуется выбрать один или несколько ответов.

            Не требуй наличия чекбокса или другого элемента выбора непосредственно
            рядом с текстом задания. Чекбоксы могут находиться только возле вариантов ответа.

            Определяй карточку по совокупности визуальных признаков:
            - отдельный текст задания или инструкции;
            - расположенные после него варианты ответа;
            - визуальная группировка задания и вариантов в одну карточку.

            После определения первой карточки с видимым текстом задания не переходи к карточкам ниже.
            Если текст этого задания обрезан, неразборчив или его нельзя надёжно восстановить,
            верни status=UNREADABLE. Не пропускай его ради более читаемого задания ниже.

            Верни status=READABLE, если текст выбранного задания и относящиеся к нему варианты
            можно надёжно прочитать. Верни status=UNREADABLE, если текст выбранного задания
            или необходимые для его распознавания варианты обрезаны, неразборчивы либо их
            нельзя уверенно отличить от текста других карточек.

            Извлекай только варианты ответа выбранной карточки. Не извлекай варианты из
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
            logger.error(
                    "Vision model request or response conversion failed; configuredModel={}",
                    model,
                    exception
            );
            throw new ScreenshotQuestionExtractionException(
                    "Vision model request failed", exception
            );
        }
    }
}
