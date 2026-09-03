# Spring AI RAG Demo -история реализации Vector Store Ingestion для изучения

## 1. Цель этой истории

Эта история фиксирует последовательность инженерных решений при реализации загрузки
Checklist Knowledge Base в векторное хранилище.

Проект постепенно проходит путь:

```text
Checklist Knowledge Base
        ↓
JSON → Java model
        ↓
ChecklistKnowledgeBaseLoader
        ↓
ChecklistQuestion
        ↓
Spring AI Document
        ↓
VectorStore
        ↓
PGVector
```

При этом отдельная задача состояла в том, чтобы:

- загрузка выполнялась автоматически при старте приложения;
- каждый checklist-вопрос был представлен как отдельный `Document`;
- вместе с текстом сохранялись `questionId` и `type`;
- было покрыто unit-тестами;
- тесты не требовали реального запуска vector ingestion;
- интеграция с Jackson 3 соответствовала используемому стеку;
- включение ingestion управлялось конфигурацией.

---

# 2. Хронология исправлений

Команда:

```bash
git log --reverse --oneline master..feat/checklist-vector-ingestion
```

показала следующие **10 содержательных коммитов**:

```text
1. 60cd632 feat(rag): load checklist knowledge base into vector store
2. 33d35b3 test(rag): verify checklist vector store ingestion
3. a80654c build(test): configure Mockito java agent
4. 41f72f6 fix(test): add Jackson databind dependency
5. 9edebd3 fix(checklist): use Jackson 3 JsonMapper
6. 8ca80fd fix(checklist): handle Jackson 3 exception
7. 0a286a2 fix(test): use Jackson 3 JsonMapper
8. 6279130 fix(test): make vector ingestion configurable
9. 0fcda4a config(app): enable vector ingestion by default
10. 44dfadc test(context): disable vector ingestion
```

Это и есть наиболее полезная последовательность для изучения.

---

# 3. Коммит №1

## `60cd632`

```text
feat(rag): load checklist knowledge base into vector store
```

### Задача

До этого момента в проекте уже существовали Checklist Knowledge Base и механизм её загрузки.

Но сами данные ещё не попадали в `VectorStore`.

То есть RAG-часть имела источник знаний, но не имела шага:

```text
Knowledge Base → Vector Store
```

### Что было добавлено

Создан:

ChecklistKnowledgeBaseVectorStoreLoader.

Он получает две зависимости:

ChecklistKnowledgeBaseLoader,
VectorStore

После события:

```text
ApplicationReadyEvent
```

вызывается:

load() .

Внутри:

```text
ChecklistKnowledgeBase knowledgeBase = knowledgeBaseLoader.load();

List<Document> documents = knowledgeBase.questions().stream()
        .map(this::toDocument)
        .toList();

vectorStore.add(documents);
```

То есть архитектурно появляется отдельный ingestion-компонент.

### Преобразование вопроса

Каждый `ChecklistQuestion` превращается в Spring AI `Document`.

Текст документа состоит из:

текст вопроса

* A) вариант A
* B) вариант B
* ...

А metadata содержит:

```text
questionId
type
```

Это непосредственно видно в реализации коммита. 

### Почему это важно

Это ключевой архитектурный момент.

Мы не кладём произвольный JSON целиком в Vector Store.

Мы сначала преобразуем доменную структуру:

```text
ChecklistQuestion
```

в структуру Spring AI:

```text
Document
```

Это разделяет ответственность:

```text
Knowledge Base
    ↓
domain model
    ↓
Spring AI Document
    ↓
VectorStore
```

### Что здесь полезного

Главный урок:

> Vector Store не должен знать о структуре исходной бизнес-модели.

Для него подготовлены документы подходящего формата.

---

# 4. Коммит №2

## `33d35b3`

```text
test(rag): verify checklist vector store ingestion
```

### Задача

После появления ingestion нужно было проверить не только то, что метод вызывается, но и **что именно отправляется в VectorStore**.

### Что сделано

Создан:

```text
ChecklistKnowledgeBaseVectorStoreLoaderTest
```

В тесте создаётся искусственная Knowledge Base с одним вопросом:

```text
Q-001
SINGLE_CHOICE
```

и двумя вариантами ответа.

Затем:

```text
ChecklistKnowledgeBaseLoader
```

и:

```text
VectorStore
```

заменяются Mockito mock-объектами.

После:

```text
loader.load();
```

тест перехватывает список `Document`, переданный в:

```text
vectorStore.add(...)
```

### Что проверяется

Проверяется количество документов:

```text
1
```

Проверяется текст вопроса.

Проверяются варианты:

```text
A) ...
B) ...
```

И самое важное - metadata:

```text
questionId = Q-001
type = SINGLE_CHOICE
```

Это видно непосредственно из теста. 

### Почему это хороший unit-тест

Здесь нет:

PostgreSQL, PGVector, Embedding API, OpenRouter.

Тест проверяет именно ответственность класса:

```text
ChecklistKnowledgeBase
        ↓
Document
        ↓
VectorStore.add()
```

### Что здесь полезного

Хороший unit-тест проверяет **контракт класса**, а не инфраструктуру вокруг него.

---

# 5. Коммит №3

## `a80654c`

```text
build(test): configure Mockito java agent
```

### Обнаружилась проблема

Для Mockito в используемой конфигурации тестов потребовалась корректная настройка Java agent.

### Что изменилось

В `pom.xml` добавлена конфигурация:

```text
maven-dependency-plugin
```

и:

```text
maven-surefire-plugin
```

с запуском Mockito как Java agent.

Также появились:

```text
<argLine></argLine> //возможно это и не обязательная строка
```

и:

```text
-javaagent:${org.mockito:mockito-core:jar}
```



### Почему это отдельный коммит

Это не изменение бизнес-логики.

Это инфраструктурная настройка тестовой среды.

Получается полезное разделение:

```text
feat(rag)
```

-- изменение приложения.

```text
test(rag)
```

-- изменение тестового покрытия.

```text
build(test)
```

-- изменение инфраструктуры запуска тестов.

### Что можно отметить 

Не все изменения в проекте должны быть `feat`.

Изменения Maven/Surefire/Mockito — это отдельный технический слой.

---

# 6. Коммит №4

## `41f72f6`

```text
fix(test): add Jackson databind dependency
```

### Что сделано

В `pom.xml` явно добавлена зависимость:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

### Зачем

В тестовом коде непосредственно используется API Jackson Databind — тест создаёт `JsonMapper`:

```text
JsonMapper.builder().build()
```

При этом `jackson-databind` мог присутствовать в проекте как транзитивная зависимость других библиотек.

Поэтому зависимость была явно указана в `pom.xml`. Это фиксирует тот факт, что код проекта непосредственно 
использует API Jackson Databind, а не просто получает эту библиотеку «по цепочке» через другие зависимости.

### Важный вывод

Наличие библиотеки в Maven `dependency tree` ещё не означает, что она является **прямой зависимостью проекта**.

Если код проекта непосредственно использует API библиотеки, явное объявление такой зависимости часто делает зависимости 
проекта понятнее и уменьшает его зависимость от случайного наличия транзитивных библиотек.

При этом **не следует автоматически объявлять все транзитивные зависимости явно**. 
Это имеет смысл прежде всего тогда, когда собственный код проекта непосредственно использует их API.

---

# 7. Коммит №5

## `9edebd3`

```text
fix(checklist): use Jackson 3 JsonMapper
```

### Проблема

Проект использует стек, где применяется Jackson 3.

Старый код использовал:

```text
com.fasterxml.jackson.databind.ObjectMapper
```

Он был заменён на:

```text
tools.jackson.databind.json.JsonMapper
```

### Изменение

Было:

```text
ObjectMapper objectMapper
```

стало:

```text
JsonMapper jsonMapper
```

И:

```text
objectMapper.readValue(...)
```

заменено на:

```text
jsonMapper.readValue(...)
```



### Архитектурный смысл

Это не изменение бизнес-модели.

Меняется инфраструктурный механизм десериализации:

```text
JSON
 ↓
Jackson 3 JsonMapper
 ↓
ChecklistKnowledgeBase
```

### Чему учиться

При миграции библиотечного стека недостаточно поменять только dependency.

Нужно проверить:

- production-код;
- тесты;
- exception types;
- конфигурацию;
- API конкретной версии библиотеки.

---

# 8. Коммит №6

## `8ca80fd`

```text
fix(checklist): handle Jackson 3 exception
```

### Проблема

После перехода на Jackson 3 изменился не только класс, используемый для чтения JSON, но и тип исключения, которое может возникнуть при обработке JSON.

Старый код использовал:

```text
IOException
```

После перехода на Jackson 3 используется:

```text
tools.jackson.core.JacksonException
```

Поэтому одного изменения `ObjectMapper` → `JsonMapper` было недостаточно. Обработку ошибок также пришлось адаптировать к новому API Jackson 3.

### Что изменилось

`ChecklistKnowledgeBaseLoader` теперь перехватывает:

```text
JacksonException
```

и преобразует её в исключение, понятное остальному приложению:

```text
IllegalStateException
```

При этом исходное исключение Jackson сохраняется как `cause`.

Условно цепочка выглядит так:

```text
JacksonException
       ↓
IllegalStateException
       ↓
"Failed to load checklist knowledge base: ..."
```

Это позволяет не терять исходную причину ошибки, но одновременно не заставляет остальной код приложения работать непосредственно с `JacksonException`.

### Что означает «локализовать внешний API»

Jackson является внешней библиотекой. `JsonMapper` и `JacksonException` относятся к её API.

В данном случае детали Jackson используются внутри `ChecklistKnowledgeBaseLoader`:

```text
ChecklistKnowledgeBaseLoader
        │
        ├── JsonMapper
        └── JacksonException
```

За пределами загрузчика приложение работает с собственными типами и общим исключением:

```text
ChecklistKnowledgeBaseLoader
        │
        │ JacksonException
        ▼
   IllegalStateException
        │
        ▼
остальное приложение
```

Поэтому остальному приложению не требуется знать, какое именно JSON-решение используется внутри загрузчика.

Если в будущем JSON-библиотека будет заменена, основная часть изменений останется внутри `ChecklistKnowledgeBaseLoader`, а не распространится по всему приложению.

### Важный вывод

Это пример **изоляции деталей внешней библиотеки внутри инфраструктурного компонента**.

Граница ответственности здесь выглядит так:

```text
Внешняя библиотека
       ↓
ChecklistKnowledgeBaseLoader
       ↓
Интерфейс/тип, используемый приложением
```

При этом речь не идёт о том, что Jackson вообще не должен быть виден в проекте. Он может свободно использоваться внутри 
компонента, который отвечает за работу с JSON.

Главная идея — **не распространять технические детали конкретной библиотеки без необходимости за пределы компонента, 
который с ней непосредственно работает**.

Локализовать внешний API — не значит спрятать библиотеку от всего проекта. Это значит ограничить места, где собственный
код непосредственно зависит от деталей этой библиотеки.

---

# 9. Коммит №7

## `0a286a2`

```text
fix(test): use Jackson 3 JsonMapper
```

### Проблема

После изменения production-кода тест продолжал создавать:

```text
new ObjectMapper()
```

То есть production и test использовали разные API.

### Исправление

Тест также переведён на:

```text
JsonMapper.builder().build()
```



### Почему это важно

Тест должен использовать тот же контракт, который используется production-кодом.

Иначе можно получить ситуацию:

```text
production → Jackson 3
test       → Jackson 2 API
```

и тест перестанет отражать реальную конфигурацию приложения.

### Чему учиться

При миграции зависимости проверяй:

```text
src/main
src/test
pom.xml
configuration
exception handling
```

---

# 10. Коммит №8

## `6279130`

```text
fix(test): make vector ingestion configurable
```

### Проблема

После появления:

```text
ChecklistKnowledgeBaseVectorStoreLoader
```

он является:

```text
@Component
```

и автоматически выполняется при:

```text
ApplicationReadyEvent
```

Это хорошо для приложения.

Но плохо для некоторых тестов.

Например, обычный:

```text
@SpringBootTest
```

теперь потенциально запускает ingestion.

### Решение

Добавлена:

```text
@ConditionalOnProperty
```

с параметром:

```text
knowledge.vector-store.ingestion.enabled
```



При этом:

```text
havingValue = "true"
matchIfMissing = true
```

### Что это даёт

Теперь lifecycle-компонент можно включать и отключать конфигурацией.

Получается:

```text
knowledge.vector-store.ingestion.enabled=true
        ↓
Loader существует
        ↓
ApplicationReadyEvent
        ↓
ingestion
```

или:

```text
knowledge.vector-store.ingestion.enabled=false
        ↓
Loader не создаётся
        ↓
ingestion не запускается
```

### Чему учиться

Это хороший пример управления инфраструктурным поведением через Spring configuration.

---

# 11. Коммит №9

## `0fcda4a`

```text
config(app): enable vector ingestion by default
```

### Что сделано

В:

```text
application.properties
```

добавлено:

```properties
knowledge.vector-store.ingestion.enabled=true
```



### Почему

Предыдущий коммит создал возможность конфигурировать ingestion.

Теперь application configuration явно говорит:

> В обычном запуске приложения ingestion должен быть включён.

### Архитектура конфигурации

Получается:

```text
application.properties
        ↓
knowledge.vector-store.ingestion.enabled
        ↓
@ConditionalOnProperty
        ↓
ChecklistKnowledgeBaseVectorStoreLoader
```

### Важный принцип

Разделены:

```text
механизм
```

и:

```text
решение о включении механизма
```

Сам класс знает, **как** выполнять ingestion.

Конфигурация определяет, **нужно ли** его включать.

---

# 12. Коммит №10

## `44dfadc`

```text
test(context): disable vector ingestion
```

### Проблема

Теперь production configuration включает ingestion:

```properties
knowledge.vector-store.ingestion.enabled=true
```

Но:

```text
@SpringBootTest
```

проверяет загрузку Spring Application Context.

### Почему это важно

`ChecklistKnowledgeBaseVectorStoreLoader` запускается на этапе жизненного цикла приложения - после готовности 
Spring-контекста.

Это удобно для реального запуска приложения: после старта контекста автоматически загружается Knowledge Base в Vector Store.

Но возникает проблема с тестами.

`@SpringBootTest` также поднимает Spring-контекст. Поэтому без дополнительной настройки тест, который всего лишь 
проверяет, что контекст приложения успешно создаётся, может автоматически запустить ingestion:

```text
@SpringBootTest
      ↓
создание Spring-контекста
      ↓
ApplicationReadyEvent
      ↓
ChecklistKnowledgeBaseVectorStoreLoader
      ↓
EmbeddingModel + PGVector
```

В результате простой context-тест начинает зависеть от внешней инфраструктуры — Vector Store и настроенной модели embeddings.

Это нежелательно: **тест, проверяющий конфигурацию и создание контекста, не должен случайно превращаться в интеграционный тест, требующий внешних сервисов.**

Поэтому ingestion был сделан отключаемым через конфигурационное свойство:

```properties
knowledge.vector-store.ingestion.enabled=false
```

А в обычной конфигурации приложения ingestion остаётся включён:

```properties
knowledge.vector-store.ingestion.enabled=true
```

Таким образом, разделяются два сценария:

```text
Обычное приложение
    ↓
Spring context
    ↓
ApplicationReadyEvent
    ↓
Vector ingestion → выполняется


Context-тест
    ↓
Spring context
    ↓
ingestion отключён → внешняя инфраструктура не требуется
```

### Чему учиться

Запуск автоматической интеграционной логики через lifecycle Spring удобен, но такую логику желательно делать **управляемой через конфигурацию**.

Это позволяет использовать один и тот же application context в разных сценариях, не заставляя каждый тест поднимать всю внешнюю инфраструктуру приложения.

---

# 13. Что произошло с архитектурой в целом

## До этой серии изменений

Была Knowledge Base:

```text
JSON
 ↓
ChecklistKnowledgeBaseLoader
 ↓
ChecklistKnowledgeBase
```

Но отсутствовал полноценный путь:

```text
Knowledge Base → VectorStore
```

---

## После первого коммита

Появился:

```text
ChecklistKnowledgeBaseVectorStoreLoader
```

и цепочка стала:

```text
JSON
 ↓
ChecklistKnowledgeBaseLoader
 ↓
ChecklistKnowledgeBase
 ↓
ChecklistQuestion
 ↓
Document
 ↓
VectorStore
```

---

# 14. Почему Document создаётся на уровне вопроса

Это одно из самых важных решений серии.

Не:

```text
вся Knowledge Base → один Document
```

а:

```text
Question 1 → Document 1
Question 2 → Document 2
Question 3 → Document 3
```

Каждый документ содержит:

```text
question text
+
answer options
+
metadata
```

Metadata:

```text
questionId
type
```

Это создаёт естественную единицу retrieval.

В будущем поиск сможет работать не просто с большим JSON-файлом, а с отдельными смысловыми единицами checklist.

---

# 15. Зачем metadata

Текст документа отвечает на вопрос:

> О чём этот документ?

Metadata отвечает на вопрос:

> Что это за объект?

Например:

```text
Document
 ├── text
 │    ├── question
 │    ├── A)
 │    └── B)
 │
 └── metadata
      ├── questionId = Q-001
      └── type = SINGLE_CHOICE
```

Это особенно важно для дальнейшего RAG pipeline.

После retrieval система может получить не просто текст, а структурированную информацию о найденном вопросе.

---

# 16. Почему ingestion выполняется через ApplicationReadyEvent

Используется:

```text
@EventListener(ApplicationReadyEvent.class)
```

Это означает:

```text
Spring application starts
        ↓
ApplicationReadyEvent
        ↓
Knowledge Base loading
        ↓
Document creation
        ↓
VectorStore.add(...)
```

Таким образом, ingestion отделён от создания Spring bean.

Сам компонент создаётся при старте контекста, а фактическая загрузка запускается после события готовности приложения.

---

# 17. Почему ingestion вынесен в отдельный класс

Можно было бы поместить загрузку Vector Store внутрь:

```text
ChecklistKnowledgeBaseLoader
```

Но это смешало бы две разные ответственности.

### `ChecklistKnowledgeBaseLoader`

Отвечает за:

```text
JSON → ChecklistKnowledgeBase
```

### `ChecklistKnowledgeBaseVectorStoreLoader`

Отвечает за:

```text
ChecklistKnowledgeBase → Document → VectorStore
```

Получается:

```text
Loader
    ↓
domain data

VectorStoreLoader
    ↓
RAG infrastructure
```

Это более чистое разделение ответственности.

---

# 18. Почему тест VectorStore не использует PGVector

В unit-тесте:

```java
VectorStore vectorStore = mock(VectorStore.class);
```

Поэтому тест не требует:

```text
PostgreSQL
PGVector
EmbeddingModel
network
```

Он проверяет именно:

```text
какие Documents сформированы
```

и:

```text
были ли они переданы в VectorStore
```

Это делает тест быстрым и детерминированным.

---

# 19. Почему понадобилась конфигурация для ingestion

Автоматический ingestion полезен в production:

```text
application starts
        ↓
knowledge base ingested
```

Но автоматический ingestion может быть нежелателен в:

```text
unit tests
context tests
локальных сценариях
```

Поэтому появилась настройка:

```properties
knowledge.vector-store.ingestion.enabled
```

И теперь lifecycle-компонент можно контролировать без изменения Java-кода.

---

# 20. Что здесь является настоящим RAG

Важно не перепутать ingestion и retrieval.

Эта серия коммитов реализует в первую очередь:

```text
INGESTION
```

То есть:

```text
Knowledge Base
      ↓
Documents
      ↓
Vector Store
```

Но это ещё не полный RAG.

Полный pipeline будет выглядеть примерно так:

```text
User screenshot/text
        ↓
Question recognition
        ↓
Structured question
        ↓
Retrieval
        ↓
Vector Store
        ↓
Relevant documents
        ↓
LLM
        ↓
Answer
```

Поэтому эта серия — фундамент retrieval-части.

---

# 21. Git-история как инженерный процесс

Эти 10 коммитов полезно воспринимать не как набор случайных исправлений, а как последовательность:

```text
1. Реализовали ingestion
        ↓
2. Добавили тест
        ↓
3. Исправили test infrastructure
        ↓
4. Уточнили Jackson dependency
        ↓
5. Исправили production API
        ↓
6. Исправили exception handling
        ↓
7. Исправили test API
        ↓
8. Добавили configuration switch
        ↓
9. Включили feature по умолчанию
        ↓
10. Отключили её в context test
```

Это нормальный реальный процесс разработки.

Не всегда сначала получается идеальная реализация.

Часто последовательность выглядит именно так:

```text
feature
 → test
 → обнаружение проблемы
 → infrastructure fix
 → dependency fix
 → API fix
 → test fix
 → configuration
 → test isolation
```

---

# 22. Что стоит запомнить из этой серии

### 1. Domain model ≠ VectorStore document

```text
ChecklistQuestion
```

преобразуется в:

```text
Document
```

---

### 2. Vector Store работает с документами

Не следует отправлять туда бизнес-модель напрямую.

---

### 3. Metadata имеет значение

```text
questionId
type
```

сохраняются отдельно от текста.

---

### 4. Unit-тест не обязан поднимать инфраструктуру

Mockito позволяет проверить:

```text
Document generation
```

без PGVector.

---

### 5. Lifecycle-компоненты требуют контроля

`ApplicationReadyEvent` удобен, но автоматическое выполнение операции может быть нежелательно в тестах.

---

### 6. Конфигурация Spring позволяет отделить механизм от его включения

```text
@ConditionalOnProperty
```

делает компонент управляемым.

---

### 7. Production и test configuration могут различаться

```text
production → ingestion=true
test       → ingestion=false
```

---

### 8. Миграция библиотеки затрагивает больше одного класса

Jackson 3 потребовал изменений:

```text
production loader
exception handling
test
dependency configuration
```

---



# 23. Итоговое состояние feature

После последнего коммита архитектура ingestion выглядит так:

```text
                  application.properties
                           │
                           │
        knowledge.vector-store.ingestion.enabled
                           │
                           ▼
              @ConditionalOnProperty
                           │
                           ▼
       ChecklistKnowledgeBaseVectorStoreLoader
                           │
                    ApplicationReadyEvent
                           │
                           ▼
              ChecklistKnowledgeBaseLoader
                           │
                           ▼
                 ChecklistKnowledgeBase
                           │
                           ▼
                  ChecklistQuestion
                           │
                           ▼
                       Document
                     ┌─────┴─────┐
                     │           │
                   text       metadata
                     │           │
                     │      questionId
                     │      type
                     │
                     └─────┬─────┘
                           ▼
                       VectorStore
                           │
                           ▼
                         PGVector
```

Именно это является главным результатом всей серии.

---

# 24. Краткая формула всей работы

Если свести все 10 коммитов к одной формуле:

```text
Knowledge Base
    ↓
Spring AI Document
    ↓
VectorStore
    ↓
tested
    ↓
configurable
    ↓
isolated from context tests
```

Это уже не просто загрузка JSON.

Это первый законченный инфраструктурный слой RAG-системы:

> **Knowledge Base → Vector Store ingestion**

А следующие этапы проекта должны строиться уже поверх него.