**[Russian version](README.ru.md)**

# Spring AI RAG Demo

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-6DB33F?logo=spring&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9.9-C71A36?logo=apachemaven&logoColor=white)

An educational project for exploring **Spring AI 2.0**, integrating LLMs, and building a RAG system (Retrieval-Augmented Generation).

The project demonstrates the gradual implementation of RAG system components: knowledge base loading, vector search, semantic question identification, and image processing using a Vision model.

## Practical Scenario

The project uses a delivery service courier checklist as its practical scenario.

When starting a shift, a courier can review delivery standards, while the application helps match a submitted question with the corresponding question in the knowledge base.

The project demonstrates how structured data, vector search, language models, and image processing can be combined in a single Spring Boot application.

## Implemented Features

- Integration of Spring AI with an OpenAI-compatible API through OpenRouter.
- Configuration of models for text processing, image processing, and embedding generation.
- Loading the Checklist Knowledge Base from a JSON file.
- Converting questions and answer options into Spring AI documents.
- Integration with PostgreSQL and PGVector.
- Loading documents into the vector store when the application starts.
- Searching for semantically similar questions using vector retrieval.
- Identifying questions with an LLM based on retrieved candidates.
- Validating the identification result against the list of retrieved candidates.
- Handling ambiguous and unidentified questions.
- Extracting question text and answer options from a screenshot using a Vision model.
- Supporting both text and image input.
- Handling errors from external AI providers and safely returning errors through the HTTP API.
- Automated tests for components and screenshot recognition scenarios.
- Local application configuration using micro-env.

## How Question Identification Works

### 1. Preparing the Knowledge Base

The Checklist Knowledge Base is loaded from a JSON file.

Each question contains:
- a unique `Question ID`;
- the question text;
- the question type;
- answer options;
- correct answer options.

For vector search, each logical question is converted into a single Spring AI document.

The document text includes the canonical question wording and its answer options. The question identifier and type are stored in the metadata.

To ensure stability across repeated loading, a deterministic technical `Document.id` is generated based on the `Question ID`.

### 2. Vector Search

When the application starts, the Checklist Knowledge Base documents are loaded into PGVector.

When a question is submitted, the application performs vector search and retrieves the most similar documents.

The search results are used as candidates for the subsequent identification stage. Vector retrieval alone is not considered a confirmed identification.

### 3. Question Identification

The LLM receives a normalized representation of the question and the list of retrieved candidates.

The application validates the result and does not allow the selection of an identifier that is absent from the supplied candidates.

Three outcomes are supported:
- `Identified` — the question has been reliably identified;
- `Ambiguous` — an ambiguous match has been found;
- `NotIdentified` — no reliable match has been found.

After successful identification, the application retrieves the canonical question record from the Checklist Knowledge Base using the `Question ID`.

If identification is ambiguous or unsuccessful, the application does not automatically select the closest result.

### 4. Screenshot Recognition

As an alternative to text input, the application accepts a checklist image.

The Vision model extracts the text of the first (topmost) question and its available answer options.

The extracted information is converted into the same normalized representation used for text input.

The same identification pipeline is then applied:
- vector search for candidates;
- semantic identification;
- result validation;
- retrieval of the canonical record from the knowledge base.

If the topmost question cannot be reliably read, the application returns an appropriate error instead of attempting to identify another question in the image.

In the current implementation, one screenshot is processed per request, and the first (topmost) question is identified.

## Web Interface

Users can:
1. Select a screenshot of a checklist question.
2. Submit the image for recognition and identification.
3. View the canonical wording of the question, the identification confidence score (if available), and the full text of the correct answer options.

The interface displays the processing status and error messages.

The current UI supports image upload only.

## Architecture

The main identification pipeline:

```text
             Text Input
                 |
                 |
Screenshot       |
    |            |
    v            |
Vision Model     |
    |            |
    +------+-----+
           |
           v
Normalized Question Representation
           |
           v
ChecklistKnowledgeBaseRetriever
           |
           v
        PGVector
           |
           v
    Candidate List
           |
           v
LlmQuestionSemanticMatcher
           |
           v
ChecklistQuestionIdentifier
           |
           v
Identification Result Validation
           |
           v
       Question ID
           |
           v
Retrieval of the Canonical Record
from the Checklist Knowledge Base
           |
           v
      HTTP Response
```

The main application components:

- `ChecklistKnowledgeBaseLoader` — loads the knowledge base.
- `ChecklistKnowledgeBaseVectorStoreLoader` — prepares and loads documents into PGVector.
- `ChecklistKnowledgeBaseRetriever` — retrieves candidates.
- `ScreenshotQuestionExtractor` — extracts a question from an image.
- `OpenRouterScreenshotVisionClient` — interacts with the Vision model.
- `LlmQuestionSemanticMatcher` — performs semantic question matching.
- `ChecklistQuestionIdentifier` — validates the identification result.
- `ChecklistService` — coordinates the process.
- `ChecklistQuestionController` — provides the REST API.

## REST API

The application provides two main ways to identify questions.

### Text Input

`POST /checklist/questions/identify`

Example request:

```json
{
  "text": "Handing the order to the guest. Select all correct statements:",
  "answerOptions": [
    "Greet the guest when meeting them",
    "Address the guest by name",
    "Wish the guest a pleasant meal"
  ],
  "selectedOptions": ["A", "B"]
}
```

### Image Input

`POST /checklist/questions/identify-screenshot`

Request type: `multipart/form-data`.

Image parameter name: `image`.

Supported formats: JPEG, PNG, and WebP.

### Processing Results

| HTTP Status                | Description                                                        |
|----------------------------|--------------------------------------------------------------------|
| `200 OK`                   | The question was successfully identified                           |
| `400 Bad Request`          | Invalid request or image                                           |
| `409 Conflict`             | Ambiguous identification                                           |
| `422 Unprocessable Entity` | The question could not be reliably identified or recognized        |
| `502 Bad Gateway`          | Invalid response from the identification service                   |
| `503 Service Unavailable`  | Search temporarily unavailable due to an embeddings provider limit |

After successful identification, the API returns the canonical question, its type, answer options, canonical `correctOptions`, and the available confidence score. Correct options are taken only from the canonical knowledge-base record associated with the confirmed `Question ID`.

The API does not check or grade user-selected answers. Correct options are not returned when identification is ambiguous or unsuccessful.

## Technology Stack

- Java 21
- Spring Boot 4.1.0
- Spring AI 2.0.0
- Spring Web
- OpenRouter
- PostgreSQL
- PGVector
- Embeddings
- Vision / multimodal LLM
- Maven
- JUnit 5
- Mockito
- Docker
- Micro-env

## Configuration

The main application settings are located in:

`src/main/resources/application.properties`

Local configuration is loaded using the [micro-env](https://central.sonatype.com/artifact/io.github.maxim618/micro-env) library.

The `micro-env.list` file defines the locations of local configuration files:

```text
secrets/openrouter=.env
secrets/db=.env
```

Example configuration files are included in the repository:

```text
secrets-example/
├── openrouter/
│   └── .env
└── db/
    └── .env
```

The following parameters are used:

| Variable             | Purpose                               |
|----------------------|---------------------------------------|
| `OPENROUTER_API_KEY` | OpenRouter access key                 |
| `DB_URL`             | PostgreSQL JDBC URL                   |
| `DB_USERNAME`        | PostgreSQL username                   |
| `DB_PASSWORD`        | PostgreSQL password                   |
| `KNOWLEDGE_FILE`     | Path to the JSON knowledge base       |
| `SCREENSHOT_MODEL`   | Model used for screenshot recognition |

Default values for some parameters are provided in `application.properties`.

## Starting PostgreSQL and PGVector

The project root contains `docker-compose.yml`.

```bash
docker compose up -d
```

## Testing

Prepared checklist screenshots and their corresponding expected results are used to test recognition.

## Project Structure

```text
spring-ai-rag-demo/
├── data/
│   └── checklist-knowledge-base.example.json
├── secrets-example/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
├── docs/
├── docker-compose.yml
├── micro-env.list
├── pom.xml
├── README.md
└── README.ru.md
```

## Further Development

Possible directions for further development:

- Generating answers with explanations.
- Additional processing of regulatory documents.
- Expanding integration tests.
- Exploring architectural approaches to RAG applications.

## Project Goal

The goal of the project is to study Spring AI and the architecture of RAG applications in Java step by step, using a specific scenario.

The main focus is on separation of responsibilities between components, verifiability of LLM results, error handling, automated testing, and making engineering decisions.

## License

The project is distributed under the MIT License.