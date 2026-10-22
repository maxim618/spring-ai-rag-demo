**[Русская версия](README.ru.md)**

# Spring AI RAG Demo

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-6DB33F?logo=spring&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9.9-C71A36?logo=apachemaven&logoColor=white)

An educational project for learning **Spring AI 2.0**, integrating LLMs and building a RAG system   
(Retrieval-Augmented Generation).

The project develops approximately as follows: from integration with a language model to loading a knowledge base, 
vector search, and identifying questions based on checklist content.

## Practical Scenario

The educational scenario uses a delivery service courier checklist.

Before starting a shift, a courier can review delivery standards, and the application helps match a submitted question  
with the corresponding question in the knowledge base.

The project makes it possible to study how structured data, vector search, and a language model can be combined in a   
single application.

## Implemented Features

- A Spring Boot application using Java 21.
- Integration with Spring AI and an OpenAI-compatible API through OpenRouter.
- Configuration of a chat model and an embedding model.
- Loading a knowledge base from a JSON file.
- Converting questions and answer options into documents for the vector store.
- Integration with PostgreSQL and PGVector.
- Loading documents into the vector store when the application starts.
- Searching for the most similar documents based on the question text.
- Checklist question identification using a language model and validation of the result against the retrieved candidates.
- Handling invalid LLM responses and testing the corresponding behavior.
- Logging technical errors when calling the model and converting its response.

## How the Process Works

### Preparing the Knowledge Base

The knowledge base is loaded from a JSON file. Each question contains an identifier, text, type, and answer options.

When preparing documents for PGVector, the question text is combined with its answer options. The question identifier   
and type are stored in the document metadata.

### Vector Search

When the application starts, the knowledge base is converted into documents and loaded into PGVector.

For an incoming question, the application searches for the three most similar documents. The retrieved documents are   
used as candidates for the next stage.

### Question Identification

The language model receives the original question and the list of retrieved candidates. The application validates the   
result: the model must not return question identifiers that are not among the supplied candidates.

The application distinguishes between successful identification, an ambiguous result, and a situation in which no reliable  
match is found.

If the model returns an invalid response or a processing error occurs, the application converts the technical   
error into a controlled exception. Internal error details are not disclosed in the HTTP response.

## Architecture

```text
JSON Knowledge Base
       │
       ▼
ChecklistKnowledgeBaseLoader
       │
       ▼
ChecklistKnowledgeBaseVectorStoreLoader
       │
       ▼
     PGVector

User Question
       │
       ▼
ChecklistKnowledgeBaseRetriever
       │
       ▼
Three Most Similar Documents
       │
       ▼
LlmQuestionSemanticMatcher
       │
       ▼
ChecklistQuestionIdentifier
       │
       ▼
Result Validation
       │
       ▼
ChecklistService
       │
       ▼
ChecklistQuestionController
       │
       ▼
HTTP Response
```

This diagram represents the current question identification process. It does not reflect the fact that the application   
already implements a separate generative response with an explanation based on the retrieved context.

## Technology Stack

- Java 21
- Spring Boot 4.1.0
- Spring AI 2.0.0
- Spring Web
- OpenRouter — access to an OpenAI-compatible API
- PostgreSQL
- PGVector
- Maven
- JUnit and Mockito for testing

## Configuration

The main settings are located in `src/main/resources/application.properties`.

The application uses the following environment variables:

| Variable             | Purpose                              |
|----------------------|--------------------------------------|
| `OPENROUTER_API_KEY` | OpenRouter access key                |
| `KNOWLEDGE_FILE`     | Path to the JSON knowledge base file |
| `DB_URL`             | PostgreSQL JDBC URL                  |
| `DB_USERNAME`        | PostgreSQL username                  |
| `DB_PASSWORD`        | PostgreSQL password                  |

Default values for `KNOWLEDGE_FILE` and the database parameters are provided in the application configuration.

The project uses the `micro-env` library to load configuration from local files; `micro-env.list` points to the `.env` file   
containing OpenRouter settings.

## Starting PostgreSQL and PGVector

The project root contains `docker-compose.yml`, which starts PostgreSQL with the PGVector extension.

```bash
docker compose up -d
```

By default, the container uses:

- Database: `rag`
- User: `rag`
- Port: `5432`

For local educational use, these values are consistent with `application.properties`.

| **Do not use such simple credentials in a publicly accessible or production environment!** |
|--------------------------------------------------------------------------------------------|



## Running the Application

Java 21, Maven, and Docker must be installed.

1. Start PostgreSQL with PGVector:

   ```bash
   docker compose up -d
   ```

2. Configure the OpenRouter key using the supported local configuration.

3. If necessary, specify a custom knowledge base file using `KNOWLEDGE_FILE`. The default is `data/checklist-knowledge-base.example.json`.

4. Start the application:

   ```bash
   mvn spring-boot:run
   ```

## Testing

Run the tests:

```bash
mvn clean test
```

Verify the build and integration tests, if they are configured in the project:

```bash
mvn clean verify
```

Component tests make it possible to verify error handling and interactions with dependencies without requiring a call   
to a real language model in every test.

## Project Structure

```text
src/
├── main/
│   ├── java/max/example/spring_ai_rag_demo/
│   │   └── checklist/
│   │       ├── ChecklistKnowledgeBaseLoader
│   │       ├── ChecklistKnowledgeBaseVectorStoreLoader
│   │       ├── ChecklistKnowledgeBaseRetriever
│   │       ├── ChecklistQuestionIdentifier
│   │       ├── LlmQuestionSemanticMatcher
│   │       ├── ChecklistService
│   │       └── ChecklistQuestionController
│   └── resources/
│       └── application.properties
├── test/
│   └── java/
└── docs/
```

Additional materials describing engineering decisions and troubleshooting are available in the project documentation.

## Further Development

Possible directions for further development:

- Separating the retrieval and answer generation stages.
- Generating answers using the retrieved context.
- Improving robustness to differences in question wording.
- Additional integration tests for vector search and configuration.
- Studying architectural approaches to RAG applications.

## Project Goal

This is an educational project for systematically learning Spring AI and the architecture of RAG applications in Java.

The primary focus is on understanding system components, the verifiability of behavior, testing, and the adoption or   
critical analysis of engineering decisions.

## License

This project is licensed under the MIT License.