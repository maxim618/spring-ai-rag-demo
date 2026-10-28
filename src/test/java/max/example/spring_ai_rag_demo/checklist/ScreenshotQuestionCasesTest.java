package max.example.spring_ai_rag_demo.checklist;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ScreenshotQuestionCasesTest {

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
        try (InputStream manifest = ScreenshotQuestionCasesTest.class
                .getResourceAsStream("/checklist/screenshot-question-cases.json")) {
            assertThat(manifest)
                    .as("Screenshot test manifest must be available on the test classpath")
                    .isNotNull();

            ScreenshotQuestionCases testCases = JsonMapper.builder()
                    .build()
                    .readValue(manifest, ScreenshotQuestionCases.class);

            assertThat(testCases.cases())
                    .as("The initial real-data test set should contain ten screenshot cases")
                    .hasSize(10);

            return testCases.cases().stream()
                    .map(testCase -> org.junit.jupiter.params.provider.Arguments.of(
                            testCase.image(),
                            testCase.expectedQuestionId()
                    ));
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
