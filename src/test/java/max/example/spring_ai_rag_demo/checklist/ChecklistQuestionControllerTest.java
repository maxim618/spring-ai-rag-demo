package max.example.spring_ai_rag_demo.checklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.OptionalDouble;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChecklistQuestionControllerTest {

    private ChecklistService checklistService;
    private ScreenshotQuestionExtractor screenshotQuestionExtractor;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        checklistService = mock(ChecklistService.class);
        screenshotQuestionExtractor = mock(ScreenshotQuestionExtractor.class);
        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new ChecklistQuestionController(
                        checklistService,
                        screenshotQuestionExtractor
                ))
                .build();
    }

    @Test
    void shouldReturnIdentifiedQuestionAndPassNormalizedInputToService() throws Exception {
        when(checklistService.identifyQuestion(any(NormalizedChecklistQuestion.class))).thenReturn(
                new ChecklistQuestionLookupResult.Identified(
                        "Q-001",
                        "SINGLE_CHOICE",
                        "Что проверить перед началом смены?",
                        List.of(new ChecklistOption("A", "Оборудование")),
                        OptionalDouble.of(0.95)
                )
        );

        mockMvc.perform(post("/checklist/questions/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "  Что проверить перед началом смены?  ",
                                  "answerOptions": ["Оборудование", "Документы"],
                                  "selectedOptions": ["Оборудование"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("Q-001"))
                .andExpect(jsonPath("$.question").value("Что проверить перед началом смены?"))
                .andExpect(jsonPath("$.options[0].text").value("Оборудование"));

        verify(checklistService).identifyQuestion(new NormalizedChecklistQuestion(
                "Что проверить перед началом смены?",
                List.of("Оборудование", "Документы"),
                List.of("Оборудование")
        ));
    }

    @Test
    void shouldReturnConflictWhenQuestionIsAmbiguous() throws Exception {
        when(checklistService.identifyQuestion(any(NormalizedChecklistQuestion.class))).thenReturn(
                new ChecklistQuestionLookupResult.Ambiguous(
                        List.of("Q-001", "Q-002"),
                        QuestionIdentificationResult.Reason.MULTIPLE_CANDIDATES,
                        OptionalDouble.empty()
                )
        );

        mockMvc.perform(post("/checklist/questions/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Что проверить?"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.candidateQuestionIds[0]").value("Q-001"));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenQuestionIsNotIdentified() throws Exception {
        when(checklistService.identifyQuestion(any(NormalizedChecklistQuestion.class))).thenReturn(
                new ChecklistQuestionLookupResult.NotIdentified(
                        List.of(),
                        QuestionIdentificationResult.Reason.NO_RELIABLE_MATCH,
                        OptionalDouble.empty()
                )
        );

        mockMvc.perform(post("/checklist/questions/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Неизвестный вопрос"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reason").value("NO_RELIABLE_MATCH"));
    }

    @Test
    void shouldRejectBlankQuestionText() throws Exception {
        mockMvc.perform(post("/checklist/questions/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(checklistService);
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/checklist/questions/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(checklistService);
    }

    @Test
    void shouldReturnServiceUnavailableWhenEmbeddingProviderRateLimitIsReached() throws Exception {
        when(checklistService.identifyQuestion(any(NormalizedChecklistQuestion.class)))
                .thenThrow(new EmbeddingProviderRateLimitException(
                        new IllegalStateException("provider rate limit details")
                ));

        mockMvc.perform(post("/checklist/questions/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"text":"Что необходимо сделать?"}
                            """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value(
                        "Question search is temporarily unavailable because the embedding provider rate limit was reached"
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("provider rate limit details")
                        )
                ));
    }

    @Test
    void shouldReturnBadGatewayWhenQuestionIdentificationFails() throws Exception {
        when(checklistService.identifyQuestion(any(NormalizedChecklistQuestion.class)))
                .thenThrow(new QuestionIdentificationException(
                        "LLM returned an invalid response",
                        new IllegalStateException("Invalid JSON")
                ));

        mockMvc.perform(post("/checklist/questions/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"text":"Что необходимо сделать?"}
                            """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value(
                        "Question identification service returned an invalid response"
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("Invalid JSON")
                        )
                ));
    }
}
