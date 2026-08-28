# Spring AI RAG Demo

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-6DB33F?logo=spring&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9.9-C71A36?logo=apachemaven&logoColor=white)

A learning project for exploring **Spring AI 2.0** and building a
**Retrieval-Augmented Generation (RAG)** application around a practical use case.

The project is developed incrementally: from a basic LLM integration towards a
RAG pipeline that can work with company-specific documents and provide answers
based on relevant source material.

## Use Case

The target scenario is a food delivery company with approximately 250 couriers.
Before starting a shift, every courier must complete a mandatory checklist to
refresh the company's delivery standards.

The standards are maintained as internal documentation. The idea is to make this
knowledge more accessible during the pre-shift training process by using a RAG-
based assistant.

The assistant is intended to:

- retrieve relevant parts of the delivery standards;
- answer courier questions using the standards as context;
- explain the applicable standard in a particular situation;
- support preparation for the mandatory pre-shift checklist;
- keep answers grounded in the company's current documentation rather than only
  relying on the general knowledge of an LLM.

The project is not intended to replace the mandatory checklist. The goal is to
explore how RAG can make a domain-specific knowledge base more useful in a
realistic operational process.

## Why RAG?

A general-purpose LLM does not know a company's internal delivery standards and
should not be expected to reproduce them reliably.

RAG separates two responsibilities:

- **Company documents** provide the domain-specific knowledge.
- **The LLM** understands the question and generates a natural-language answer
  using the retrieved context.

This makes the courier training scenario a practical example of using RAG with
private, domain-specific and changeable information.

## Current Status

The project is currently in the early development stage.

Implemented:

- Basic Spring Boot application
- Spring AI integration
- `ChatClient` configuration
- OpenAI-compatible API integration
- Simple chat endpoint

Planned:

- Document ingestion
- Document chunking
- Embeddings
- Vector store integration
- Retrieval
- RAG-based prompt augmentation
- Domain-specific documents containing delivery standards
- End-to-end RAG pipeline
- Tests for the RAG components

## Architecture

The target RAG workflow is:

```text
Company Standards
       │
       ▼
Document Ingestion
       │
       ▼
Document Chunking
       │
       ▼
   Embeddings
       │
       ▼
  Vector Store
       │
       │ relevant context
       ▼
Courier Question
       │
       ▼
   Retrieval
       │
       ▼
Relevant Documents
       │
       ▼
      LLM
       │
       ▼
     Answer
```

At the current stage, the retrieval and vector-store components have not yet
been implemented.

## Technology Stack

- Java 21
- Spring Boot 4.1.0
- Spring AI 2.0.0
- Spring Web
- OpenAI-compatible API
- Maven

## Configuration

The application uses Spring AI's OpenAI integration and can be configured to
work with an OpenAI-compatible API.

API credentials should be supplied through environment variables and must not be
committed to the repository.

Example:

```bash
export OPENAI_API_KEY=your-api-key
```

Provider and model settings can be configured in `application.properties`.

## Running the Application

### Prerequisites

- Java 21
- Maven 3.9+
- An OpenAI-compatible API key

### Start the application

```bash
mvn spring-boot:run
```

## Project Structure

```text
src/
└── main/
    ├── java/
    │   └── com/max/springairagdemo/
    │       ├── SpringAiRagDemoApplication.java
    │       └── service/
    │           └── ChatService.java
    │
    └── resources/
        └── application.properties
```

The structure will evolve as the RAG pipeline is introduced.

## Roadmap

- [x] Create Spring Boot application
- [x] Integrate Spring AI
- [x] Configure `ChatClient`
- [x] Connect to an OpenAI-compatible API
- [ ] Prepare a representative set of delivery standards
- [ ] Add document ingestion
- [ ] Add document chunking
- [ ] Generate embeddings
- [ ] Add a vector store
- [ ] Implement document retrieval
- [ ] Build the RAG pipeline
- [ ] Connect retrieval to the courier training use case
- [ ] Add tests for the RAG components

## Purpose

This repository is primarily a **learning and experimentation project**.

The goal is to understand how the components of a RAG application fit together
and how Spring AI can be used to build an AI assistant grounded in
company-specific documentation.

The courier training scenario provides a concrete domain for the experiment,
allowing the technical implementation to be evaluated against a realistic
business problem rather than as an isolated RAG demo.

The implementation is intentionally incremental. The project starts with a
simple LLM integration and gradually introduces retrieval, vector storage and
other RAG components.

## License

This project is licensed under the MIT License.

**[Русская версия](README.ru.md)**
