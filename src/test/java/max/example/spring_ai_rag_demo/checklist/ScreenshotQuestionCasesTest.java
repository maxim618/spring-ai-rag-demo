package max.example.spring_ai_rag_demo.checklist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ScreenshotQuestionCasesTest {

    private static final List<String> EXPECTED_QUESTION_IDS = List.of(
            "Q-001", "Q-002", "Q-003", "Q-004",
            "Q-005", "Q-006", "Q-007", "Q-008",
            "Q-009", "Q-010", "Q-011", "Q-012"
    );

    @Test
    void shouldKeepGoldenScreenshotManifestConsistentWithoutCallingVisionApi()
            throws IOException {
        List<ScreenshotQuestionCase> cases = loadCases();

        assertThat(cases)
                .as("The golden screenshot regression set should contain all twelve cases")
                .hasSize(12);

        assertThat(cases)
                .extracting(ScreenshotQuestionCase::expectedQuestionId)
                .containsExactlyElementsOf(EXPECTED_QUESTION_IDS);

        List<String> imageNames = cases.stream()
                .map(ScreenshotQuestionCase::image)
                .toList();

        assertThat(new HashSet<>(imageNames))
                .as("Each golden case must reference a unique screenshot")
                .hasSameSizeAs(imageNames);
    }

    @ParameterizedTest(name = "{0} should have expected question ID {1}")
    @MethodSource("screenshotCases")
    void shouldHaveScreenshotFixtureForExpectedQuestion(
            String imageName,
            String expectedQuestionId
    ) throws IOException {
        try (InputStream image = getClass().getResourceAsStream(
                "/checklist/screenshots/" + imageName
        )) {
            assertThat(image)
                    .as("Screenshot fixture %s must be available on the test classpath", imageName)
                    .isNotNull();
        }

        assertThat(imageName)
                .as("The fixture name should match the expected topmost question ID")
                .isEqualTo(expectedQuestionId + ".jpg");
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> screenshotCases()
            throws IOException {
        return loadCases().stream()
                .map(testCase -> org.junit.jupiter.params.provider.Arguments.of(
                        testCase.image(),
                        testCase.expectedQuestionId()
                ));
    }

    private static List<ScreenshotQuestionCase> loadCases() throws IOException {
        try (InputStream manifest = ScreenshotQuestionCasesTest.class
                .getResourceAsStream("/checklist/screenshot-question-cases.json")) {
            assertThat(manifest)
                    .as("Screenshot test manifest must be available on the test classpath")
                    .isNotNull();

            ScreenshotQuestionCases testCases = JsonMapper.builder()
                    .build()
                    .readValue(manifest, ScreenshotQuestionCases.class);

            assertThat(testCases.cases())
                    .as("Screenshot test manifest must contain a cases array")
                    .isNotNull();

            return testCases.cases();
        }
    }

    private record ScreenshotQuestionCases(List<ScreenshotQuestionCase> cases) {
    }

    private record ScreenshotQuestionCase(
            String image,
            String expectedQuestionId
    ) {
    }
}
