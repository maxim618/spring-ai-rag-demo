![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-6DB33F?logo=spring&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9.9-C71A36?logo=apachemaven&logoColor=white)


# Spring AI RAG Demo

A small learning project for exploring **Spring AI 2.0** and building a
**Retrieval-Augmented Generation (RAG)** application.

The project is developed incrementally, starting with a basic LLM integration
and gradually introducing the components required for a RAG pipeline.

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
- End-to-end RAG pipeline

## Architecture

The application is being built around the following RAG workflow:

```text
User Question
      │
      ▼
  Retrieval
      │
      ▼
 Vector Store
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


## Project Structure
```text
src/
└── main/
    ├── java/
    │   └── com/max/springairagdemo/
    │       ├── SpringAiRagDemoApplication.java
    │       └── service/
    │           └── ChatService.java
    └── resources/
        └── application.properties
```

### Пройденные этапы

- [x] Создание Spring Boot приложения
- [x] Интеграция Spring AI
- [x] Настройка `ChatClient`
- [x] Подключение к OpenAI-совместимому API
- [ ] Настройка загрузки документов (Document Ingestion)
- [ ] Настройка разбиения документов на части (Document Chunking)
- [ ] Генерация эмбеддингов (Embeddings)
- [ ] Подключение векторного хранилища (Vector Store)
- [ ] Реализация поиска документов (Document Retrieval)
- [ ] Сборка готового RAG-конвейера (RAG Pipeline)
- [ ] Написание тестов для компонентов RAG


**[Русская версия](README.ru.md)**



