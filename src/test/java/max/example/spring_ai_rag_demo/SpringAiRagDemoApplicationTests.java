package max.example.spring_ai_rag_demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "knowledge.vector-store.ingestion.enabled=false"
})
class SpringAiRagDemoApplicationTests {

    @Test
    void contextLoads() {
    }

}
