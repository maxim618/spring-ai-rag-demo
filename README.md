**[Русская версия](README.ru.md)**

# Spring AI RAG Demo

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-6DB33F?logo=spring&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9.9-C71A36?logo=apachemaven&logoColor=white)

A learning project for exploring Spring AI 2.0 with
Retrieval-Augmented Generation (RAG).

The project is developed incrementally: from a basic LLM integration towards a
RAG pipeline that can provide relevant answers to questions.

## Use Case

The target scenario is a food delivery company.

Before starting a shift, couriers complete a checklist to refresh their delivery standards.

The assistant is intended to answer questions related to delivery standards.

The assistant is intended to:

- retrieve relevant parts of the delivery standards;
- answer courier questions using the retrieved context;
- explain the applicable standard in a particular situation.

## Why RAG?

A general-purpose LLM may not have the specific knowledge required for a particular domain.

RAG allows relevant context to be used when generating an answer while preserving a natural interaction with the LLM.

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
- Materials containing delivery standards
- End-to-end RAG pipeline
- Tests for the RAG components

## Architecture

The target RAG workflow is:

```text
Materials
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
Relevant Context
       │
       ▼
      LLM
       │
       ▼
     Answer
````

At the current stage, the retrieval and vector-store components have not yet
been implemented.

## Technology Stack

* Java 21
* Spring Boot 4.1.0
* Spring AI 2.0.0
* Spring Web
* OpenAI-compatible API
* Maven

## Configuration

The application uses Spring AI's OpenAI integration and can be configured to
work with an OpenAI-compatible API.

API credentials should be supplied through environment variables.

Provider and model settings can be configured in `application.properties`.

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

* [x] Create Spring Boot application
* [x] Integrate Spring AI
* [x] Configure `ChatClient`
* [x] Connect to an OpenAI-compatible API
* [ ] Prepare a representative set of delivery standards
* [ ] Add document ingestion
* [ ] Add document chunking
* [ ] Generate embeddings
* [ ] Add a vector store
* [ ] Implement document retrieval
* [ ] Build the RAG pipeline
* [ ] Connect retrieval to the courier use case
* [ ] Add tests for the RAG components

## Purpose

This repository is primarily a learning project.

The goal is to understand Spring AI 2.0 and RAG.

The implementation is intentionally incremental. The project starts with a
simple LLM integration and gradually introduces retrieval, vector storage and
other RAG components.

## License

This project is licensed under the MIT License.

