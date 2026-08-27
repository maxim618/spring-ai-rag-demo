 **[English version](README.md)**
 
## Spring AI RAG DEMO
Учебный проект для изучения Spring AI 2.0 и создания приложения с Retrieval-Augmented Generation (RAG).
Проект развивается постепенно: мы начинаем с базовой интеграции LLM и пошагово внедряем компоненты, необходимые для полноценного RAG-конвейера.
## Текущий статус
Проект находится на ранней стадии разработки.
Реализовано:

* Базовое приложение на Spring Boot.
* Интеграция со Spring AI.
* Настройка ChatClient.
* Интеграция с OpenAI-совместимыми API.
* Простой эндпоинт для чата.

Планируется:

* Загрузка документов (Document ingestion).
* Разбиение документов на части (Document chunking).
* Векторные представления (Embeddings).
* Интеграция с векторным хранилищем (Vector store).
* Поиск данных (Retrieval).
* Дополнение  промптов на основе RAG.
* Сборка сквозного RAG-конвейера.

## Архитектура
Приложение строится на основе примерно такого workflow:

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

## Структура проекта

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


