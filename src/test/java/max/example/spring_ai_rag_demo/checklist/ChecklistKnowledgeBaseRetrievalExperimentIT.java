
package max.example.spring_ai_rag_demo.checklist;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "knowledge.vector-store.ingestion.enabled=true"
})
class ChecklistKnowledgeBaseRetrievalExperimentIT {

    @Autowired
    private ChecklistKnowledgeBaseRetriever retriever;

    private record QueryCase(
            String query,
            String category,
            String frequency,
            Set<String> expectedQuestionIds
    ) {}

    private record CategoryStats(
            int evaluated,
            int matched
    ) {}

    @Test
    void shouldEvaluateRetrievalAcrossDiagnosticScenarios() {
        List<QueryCase> cases = List.of(
                new QueryCase(
                        "Что необходимо сделать перед началом рабочего дня?",
                        "COMMON_EXACT",
                        "Основной сценарий",
                        Set.of("Q-001")
                ),
                new QueryCase(
                        "Какие действия помогают обеспечить корректное выполнение заказа?",
                        "COMMON_EXACT",
                        "Основной сценарий",
                        Set.of("Q-002")
                ),
                new QueryCase(
                        "Какие варианты поведения соответствуют профессиональному общению с клиентом?",
                        "COMMON_EXACT",
                        "Основной сценарий",
                        Set.of("Q-003")
                ),
                new QueryCase(
                        "Что нужно проверить до начала смены?",
                        "RARE_PARAPHRASE",
                        "Примерно раз в 4–6 месяцев",
                        Set.of("Q-001")
                ),
                new QueryCase(
                        "Перед выходом на работу какие действия обязательны?",
                        "RARE_PARAPHRASE",
                        "Примерно раз в 4–6 месяцев",
                        Set.of("Q-001")
                ),
                new QueryCase(
                        "Как убедиться, что заказ выполнен правильно?",
                        "RARE_PARAPHRASE",
                        "Примерно раз в 4–6 месяцев",
                        Set.of("Q-002")
                ),
                new QueryCase(
                        "Как следует разговаривать с клиентом?",
                        "RARE_PARAPHRASE",
                        "Примерно раз в 4–6 месяцев",
                        Set.of("Q-003")
                ),
                new QueryCase(
                        "Что нужно проверить?",
                        "AMBIGUOUS",
                        "Не определена",
                        Set.of("Q-001", "Q-002")
                ),
                new QueryCase(
                        "Как оформить возврат денег клиенту?",
                        "OUT_OF_SCOPE",
                        "Не определена",
                        Set.of()
                ),
                new QueryCase(
                        "Куда обращаться при поломке автомобиля?",
                        "OUT_OF_SCOPE",
                        "Не определена",
                        Set.of()
                ),
                new QueryCase(
                        "Что проверить и как вежливо общаться с клиентом?",
                        "MULTI_TOPIC",
                        "Не определена",
                        Set.of("Q-001", "Q-003")
                )
        );

        var statsByCategory = new java.util.LinkedHashMap<String, int[]>();

        for (QueryCase testCase : cases) {
            List<Document> documents =
                    retriever.retrieve(testCase.query());

            System.out.println();
            System.out.println("==================================================");
            System.out.println("Query: " + testCase.query());
            System.out.println("Category: " + testCase.category());
            System.out.println("Frequency: " + testCase.frequency());
            System.out.println(
                    "Expected question IDs: "
                            + testCase.expectedQuestionIds()
            );

            if (documents.isEmpty()) {
                System.out.println("Retrieval result: NO_DOCUMENTS");
                System.out.println("Evaluation: NOT_EVALUATED");
                continue;
            }

            String firstId = questionId(documents.getFirst());

            System.out.println("Top-rank ID: " + firstId);

            switch (testCase.category()) {
                case "COMMON_EXACT", "RARE_PARAPHRASE" -> {
                    boolean matched =
                            testCase.expectedQuestionIds().contains(firstId);

                    int[] stats = statsByCategory.computeIfAbsent(
                            testCase.category(),
                            key -> new int[2]
                    );

                    stats[0]++;
                    if (matched) {
                        stats[1]++;
                    }

                    System.out.println(
                            "Top-rank match: " + matched
                    );
                    System.out.println(
                            "Evaluation: "
                                    + (matched ? "PASS" : "FAIL")
                    );
                }
                case "AMBIGUOUS" -> {
                    System.out.println(
                            "Evaluation: NOT_EVALUATED"
                    );
                    System.out.println(
                            "Reason: ambiguity handling is not implemented"
                    );
                }
                case "OUT_OF_SCOPE" -> {
                    System.out.println(
                            "Evaluation: NOT_EVALUATED"
                    );
                    System.out.println(
                            "Reason: out-of-scope rejection is not implemented"
                    );
                }
                case "MULTI_TOPIC" -> {
                    System.out.println(
                            "Evaluation: NOT_EVALUATED"
                    );
                    System.out.println(
                            "Reason: multi-topic coverage is not evaluated"
                    );
                }
                default -> throw new IllegalStateException(
                        "Unknown category: " + testCase.category()
                );
            }

            for (int i = 0; i < documents.size(); i++) {
                Document document = documents.get(i);

                System.out.printf(
                        "%d. questionId=%s, distance=%s%n",
                        i + 1,
                        questionId(document),
                        document.getMetadata().get("distance")
                );
            }
        }

        System.out.println();
        System.out.println("==================================================");
        System.out.println("SUMMARY BY CATEGORY");

        statsByCategory.forEach((category, stats) -> {
            double matchRate = stats[0] == 0
                    ? 0.0
                    : 100.0 * stats[1] / stats[0];

            System.out.printf(
                    "%s: %d/%d matches (%.1f%%)%n",
                    category,
                    stats[1],
                    stats[0],
                    matchRate
            );
        });

        System.out.println(
                "AMBIGUOUS, OUT_OF_SCOPE and MULTI_TOPIC: "
                        + "not scored; corresponding behavior is not implemented"
        );
    }

    private static String questionId(Document document) {
        Object value = document.getMetadata().get("questionId");
        return value == null ? null : value.toString();
    }
}
