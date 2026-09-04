# Диагностика и устранение предупреждения Mockito self-attach при запуске тестов на Java 21

## 1. Исходная проблема

При запуске тестов Maven появлялось предупреждение:

```text
Mockito is currently self-attaching to enable the inline-mock-maker.
This will no longer work in future releases of the JDK.
Please add Mockito as an agent to your build as described in Mockito's documentation.
```

Также JVM выводила:

```text
WARNING: A Java agent has been loaded dynamically
...
WARNING: Dynamic loading of agents will be disallowed by default in a future release
```

И сообщение:

```text
OpenJDK 64-Bit Server VM warning:
Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

Главный вопрос:

> Почему Mockito самостоятельно подключает Java Agent и как правильно устранить причину предупреждения, а не просто скрыть его?

---

# 2. Что важно понимать до диагностики

Современный Mockito использует **inline mock maker** для создания некоторых видов mock-объектов.

Для этого Mockito необходим доступ к Java Instrumentation API.

Есть два принципиально разных способа получить instrumentation:

### Вариант 1. Dynamic attach

Mockito во время выполнения пытается самостоятельно присоединить Java Agent к уже работающей JVM.

Упрощённая схема:

```text
Запуск теста
    ↓
Mockito
    ↓
Instrumentation ещё нет
    ↓
Byte Buddy Agent
    ↓
dynamic attach
    ↓
JVM загружает agent
```

Именно этот механизм и вызывает предупреждение:

```text
Mockito is currently self-attaching...
```

---

### Вариант 2. Agent подключён заранее

JVM запускается сразу с:

```text
-javaagent:mockito-core-5.23.0.jar
```

Схема:

```text
Запуск JVM
    ↓
-javaagent:mockito-core-5.23.0.jar
    ↓
Mockito получает Instrumentation
    ↓
тест
```

В этом случае Mockito не должен выполнять self-attach.

**Именно второй вариант нам и нужен.**

---

# 3. Почему проблема стала актуальной именно сейчас

Проект использует:

```text
Java 21
```

А современные версии JDK постепенно ужесточают отношение к динамической загрузке Java Agents.

Поэтому старый подход:

```text
Mockito → самостоятельно подключить agent
```

становится нежелательным.

Правильный современный подход:

```text
система сборки → заранее передать Mockito как -javaagent
```

---

# 4. Первый важный вопрос: действительно ли Mockito использует dynamic attach?

Сначала нужно было не гадать, а доказать механизм.

Мы исследовали содержимое Mockito:

```bat
jar tf %USERPROFILE%\.m2\repository\org\mockito\mockito-core\5.23.0\mockito-core-5.23.0.jar | findstr /I "MockMaker"
```

## Для чего команда?

`jar tf` показывает содержимое JAR-файла.

`findstr` оставляет только строки, содержащие:

```text
MockMaker
```

Это позволило увидеть классы, связанные с механизмом создания mock-объектов.

---

# 5. Проверка внутреннего механизма Mockito

Следующим шагом мы посмотрели байткод класса:

```text
org.mockito.internal.creation.bytebuddy.InlineDelegateByteBuddyMockMaker
```

Команда:

```bat
javap -private -c -classpath %USERPROFILE%\.m2\repository\org\mockito\mockito-core\5.23.0\mockito-core-5.23.0.jar org.mockito.internal.creation.bytebuddy.InlineDelegateByteBuddyMockMaker
```

## Для чего?

`javap` позволяет посмотреть структуру Java-класса и его байткод.

Нас интересовало, откуда Mockito получает:

```text
java.lang.instrument.Instrumentation
```

В результате обнаружился вызов:

```text
org.mockito.internal.PremainAttachAccess.getInstrumentation()
```

То есть проблема действительно связана с получением instrumentation.

---

# 6. Исследование PremainAttachAccess

Затем был исследован класс:

```text
org.mockito.internal.PremainAttachAccess
```

Команда:

```bat
javap -private -c -classpath %USERPROFILE%\.m2\repository\org\mockito\mockito-core\5.23.0\mockito-core-5.23.0.jar org.mockito.internal.PremainAttachAccess
```

Внутри обнаружилась логика:

```text
net.bytebuddy.agent.Installer
```

и:

```text
net.bytebuddy.agent.ByteBuddyAgent.install()
```

а также непосредственно текст предупреждения:

```text
Mockito is currently self-attaching...
```

Это стало важным доказательством.

Получилась цепочка:

```text
Mockito
   ↓
PremainAttachAccess
   ↓
Instrumentation отсутствует
   ↓
ByteBuddyAgent.install()
   ↓
dynamic attach
   ↓
Mockito warning
```

---

# 7. Важное открытие: Mockito уже содержит Java Agent

После этого возник естественный вопрос:

> А есть ли вообще у Mockito собственный premain-agent?

Мы исследовали:

```text
org.mockito.internal.PremainAttach
```

Команда:

```bat
javap -private -c -classpath %USERPROFILE%\.m2\repository\org\mockito\mockito-core\5.23.0\mockito-core-5.23.0.jar org.mockito.internal.PremainAttach
```

Было обнаружено:

```text
public static void premain(...)
```

То есть Mockito действительно умеет работать как Java Agent.

---

# 8. Проверка MANIFEST.MF

Для окончательного подтверждения посмотрели manifest:

```bat
jar xf %USERPROFILE%\.m2\repository\org\mockito\mockito-core\5.23.0\mockito-core-5.23.0.jar META-INF/MANIFEST.MF
type META-INF\MANIFEST.MF | findstr /I "Premain Agent Retransform"
```

Было обнаружено:

```text
Premain-Class: org.mockito.internal.PremainAttach
Can-Retransform-Classes: true
```

Это окончательно подтвердило:

> `mockito-core-5.23.0.jar` может быть непосредственно передан JVM как `-javaagent`.

Поэтому нам не требуется искать какой-то отдельный Mockito Agent JAR.

---

# 9. Где должен подключаться Agent?

Следующий вопрос:

> Кто запускает тесты?

В проекте тесты запускаются Maven через:

```text
maven-surefire-plugin
```

Именно Surefire запускает отдельный JVM-процесс для тестов.

Поэтому Java Agent необходимо передать в параметры JVM, которую запускает Surefire.

---

# 10. Почему нельзя просто добавить зависимость Mockito

В проекте Mockito уже присутствует через тестовые зависимости.

Но наличие:

```xml
<dependency>
    ...
</dependency>
```

не означает:

```text
java -javaagent:mockito-core.jar
```

Это принципиально разные вещи.

### Dependency

Сообщает Maven:

> этот JAR нужен проекту.

### `-javaagent`

Сообщает JVM:

> загрузи этот JAR как Java Agent ещё до запуска приложения.

Поэтому одной зависимости недостаточно.

---

# 11. Проверка текущего POM

В `pom.xml` была добавлена настройка:

```xml
<argLine></argLine>
```

и:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>properties</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Затем:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
    </configuration>
</plugin>
```

---

# 12. Зачем нужен dependency-plugin

Нас интересует:

```text
${org.mockito:mockito-core:jar}
```

Это Maven property, содержащий путь к JAR-файлу Mockito.

После выполнения:

```xml
<goal>properties</goal>
```

Maven может использовать путь:

```text
${org.mockito:mockito-core:jar}
```

в конфигурации Surefire.

В результате:

```text
-javaagent:${org.mockito:mockito-core:jar}
```

превращается примерно в:

```text
-javaagent:C:\Users\User\.m2\repository\org\mockito\mockito-core\5.23.0\mockito-core-5.23.0.jar
```

---

# 13. Проверяем, действительно ли Surefire получил Agent

Просто посмотреть POM недостаточно.

Нужно проверить фактическую команду запуска JVM.

Для этого использовали:

```bat
mvn -Dtest=ChecklistKnowledgeBaseVectorStoreLoaderTest -X test > test-debug.txt 2>&1
```

## Что делает команда?

```text
-Dtest=...
```

Запускает только конкретный тест.

```text
-X
```

включает подробный debug-вывод Maven.

```text
> test-debug.txt 2>&1
```

сохраняет весь вывод в файл:

```text
test-debug.txt
```

---

# 14. Поиск нужной информации в debug-логе

Затем:

```bat
findstr /I /C:"javaagent" /C:"argLine =" /C:"Forking command line" test-debug.txt
```

## Для чего?

Нам не нужен весь огромный debug-лог.

Нужно найти:

```text
argLine
```

и:

```text
Forking command line
```

Именно там можно увидеть реальные JVM arguments.

Получили:

```text
[DEBUG] (s) argLine = @{argLine} -javaagent:C:\Users\User\.m2\repository\org\mockito\mockito-core\5.23.0\mockito-core-5.23.0.jar
```

И самое важное:

```text
[DEBUG] Forking command line:
cmd.exe /X /C "C:\java\graalvm-ce-21\bin\java
-javaagent:C:\Users\User\.m2\repository\org\mockito\mockito-core\5.23.0\mockito-core-5.23.0.jar
-jar ..."
```

---

# 15. Ключевой вывод

Это был переломный момент расследования.

Мы получили доказательство:

```text
POM
 ↓
Surefire
 ↓
-javaagent:mockito-core-5.23.0.jar
 ↓
JVM
```

То есть Maven **не игнорирует** нашу настройку.

Agent действительно передаётся JVM.

---

# 16. Контрольная проверка через явный `-DargLine`

Для дополнительного доказательства отдельно запустили:

```bat
mvn -Dtest=ChecklistKnowledgeBaseVectorStoreLoaderTest -DargLine="-javaagent:%USERPROFILE%\.m2\repository\org\mockito\mockito-core\5.23.0\mockito-core-5.23.0.jar" test
```

Здесь Agent передавался непосредственно через Maven property.

Результат:

```text
Tests run: 1, Failures: 0, Errors: 0
BUILD SUCCESS
```

И предупреждение:

```text
Mockito is currently self-attaching
```

исчезло.

Осталось только:

```text
Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

Это подтвердило причинно-следственную связь:

```text
Agent заранее подключён
        ↓
Mockito self-attach не требуется
        ↓
Mockito warning исчезает
```

---

# 17. Проверка обычного `mvn test`

После этого снова был выполнен обычный:

```bat
mvn test
```

Результат:

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
```

Для:

```text
ChecklistKnowledgeBaseVectorStoreLoaderTest
```

осталось только:

```text
OpenJDK 64-Bit Server VM warning:
Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

А предупреждения Mockito больше нет.

---

# 18. Как отдельно проверить отсутствие Mockito warning

Использовали:

```bat
findstr /I /C:"Mockito is currently" /C:"dynamically" /C:"Byte Buddy" /C:"Sharing is only" test-debug-2.txt
```

Результат:

```text
OpenJDK 64-Bit Server VM warning:
Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

Важный момент:

```text
Mockito is currently
dynamically
Byte Buddy
```

не найдены.

Следовательно, dynamic self-attach устранён.

---

# 19. Что означает оставшееся предупреждение

Оставшееся:

```text
Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

не означает:

```text
Mockito неправильно работает
```

и не означает:

```text
Mockito снова делает dynamic attach
```

Это сообщение JVM связано с:

```text
Class Data Sharing (CDS)
```

и изменением classpath/инструментированием JVM.

То есть после исправления Mockito мы получили другую, отдельную категорию предупреждения.

---

# 20. Очень важное правило диагностики

Нельзя объединять несколько warning в один только потому, что они появляются рядом.

В нашем случае было:

```text
WARNING A:
Mockito is currently self-attaching
```

и:

```text
WARNING B:
Sharing is only supported...
```

После исправления Agent:

```text
WARNING A → исчез
WARNING B → остался
```

Это очень хороший диагностический признак.

Он показывает, что предупреждения имеют разные причины.

---

# 21. Почему `-XX:+EnableDynamicAgentLoading` — не наше решение

JVM предлагает параметр:

```text
-XX:+EnableDynamicAgentLoading
```

Он позволяет разрешить динамическую загрузку Java Agents.

Также он может использоваться для подавления соответствующего предупреждения.

Но это **не устраняет первопричину**.

Мы хотим:

```text
Mockito self-attach
       ↓
не происходит
```

а не:

```text
Mockito self-attach
       ↓
происходит
       ↓
JVM предупреждение скрыто
```

Поэтому параметр не является правильным решением нашей задачи.

---

# 22. Почему мы не стали использовать `-Djdk.instrument.traceUsage`

Это диагностический параметр JVM:

```text
-Djdk.instrument.traceUsage
```

Он нужен для отслеживания использования instrumentation API.

Но после того как мы уже установили механизм через:

```text
PremainAttachAccess
        ↓
ByteBuddyAgent.install()
```

добавлять его в постоянную конфигурацию нет необходимости.

Это был бы диагностический инструмент, а не исправление.

---

# 23. Что делать с `<argLine></argLine>`?

В POM была строка:

```xml
<argLine></argLine>
```

Она выглядит подозрительно, потому что значение пустое.

Важно понимать назначение:

```xml
<argLine>@{argLine} -javaagent:...</argLine>
```

Конструкция:

```text
@{argLine}
```

используется Maven Surefire как **late replacement** — то есть позволяет сохранить значение `argLine`, которое могло быть установлено другим Maven-плагином.

Например, подобный механизм часто нужен для совместной работы с инструментами покрытия кода.

В нашем текущем проекте отдельного плагина, который устанавливает `argLine`, нет.

Поэтому пустой:

```xml
<argLine></argLine>
```

сам по себе не является Java Agent и не делает никакой полезной работы для Mockito.

Однако прежде чем удалять его, конфигурацию нужно проверять экспериментально.

Главный принцип:

> Не удалять элемент только потому, что он выглядит пустым. Сначала проверить, зачем он участвует в цепочке Maven configuration.

---

# 24. Важное различие: Maven и IntelliJ IDEA

В процессе расследования обнаружилось ещё одно важное обстоятельство.

Команда:

```bat
mvn test
```

использует:

```text
Maven
 ↓
Surefire
 ↓
отдельная JVM
```

И поэтому Maven применяет:

```xml
<argLine>...</argLine>
```

А запуск теста непосредственно из IntelliJ IDEA может идти иначе:

```text
IntelliJ IDEA
 ↓
JUnit launcher
 ↓
JVM
```

В таком случае Maven Surefire вообще не участвует.

Следовательно:

```text
maven-surefire-plugin
```

не обязан влиять на запуск теста из IntelliJ IDEA.

---

# 25. Наблюдение при запуске из IntelliJ IDEA

При отдельном запуске:

```text
ChecklistKnowledgeBaseVectorStoreLoaderTest
```

из IntelliJ IDEA предупреждение Mockito появляется снова.

Это согласуется с архитектурой запуска:

```text
Maven:

pom.xml
  ↓
Surefire
  ↓
-javaagent:mockito-core.jar
  ↓
Mockito
  ↓
нет self-attach warning
```

против:

```text
IntelliJ:

Run Test
  ↓
JUnit launcher
  ↓
Mockito
  ↓
agent заранее не передан
  ↓
self-attach
  ↓
warning
```

Поэтому это **не доказывает неисправность Maven-конфигурации**.

Наоборот, это показывает, что существуют два разных способа запуска тестов.

---

# 26. Полная цепочка нашего расследования

Весь процесс можно представить так:

```text
1. Получили warning
        ↓
2. Не стали его подавлять
        ↓
3. Определили, что warning связан с Mockito
        ↓
4. Исследовали mock maker
        ↓
5. Нашли PremainAttachAccess
        ↓
6. Нашли ByteBuddyAgent.install()
        ↓
7. Доказали механизм dynamic attach
        ↓
8. Исследовали PremainAttach
        ↓
9. Доказали наличие premain Java Agent
        ↓
10. Проверили MANIFEST.MF
        ↓
11. Увидели Premain-Class
        ↓
12. Настроили Surefire
        ↓
13. Добавили Mockito как -javaagent
        ↓
14. Запустили Maven с -X
        ↓
15. Проверили реальную JVM command line
        ↓
16. Убедились, что -javaagent действительно передаётся
        ↓
17. Повторно запустили тест
        ↓
18. Mockito warning исчез
        ↓
19. Проверили полный mvn test
        ↓
20. Все 3 теста прошли
        ↓
21. Осталось только отдельное JVM warning про CDS
        ↓
22. Обнаружили отдельную проблему запуска из IntelliJ
```

---

# 27. Полезные команды — краткая шпаргалка

## Запустить все тесты

```bat
mvn test
```

Назначение:

> Проверить состояние всего тестового набора.

---

## Запустить один тест

```bat
mvn -Dtest=ChecklistKnowledgeBaseVectorStoreLoaderTest test
```

Назначение:

> Изолировать конкретный тест и убрать шум от остальных.

---

## Получить подробный Maven debug

```bat
mvn -Dtest=ChecklistKnowledgeBaseVectorStoreLoaderTest -X test > test-debug.txt 2>&1
```

Назначение:

> Исследовать фактическую конфигурацию Maven и команду запуска тестовой JVM.

---

## Найти JVM arguments

```bat
findstr /I /C:"argLine =" /C:"Forking command line" /C:"javaagent" test-debug.txt
```

Назначение:

> Проверить, действительно ли Surefire передал `-javaagent`.

---

## Найти конкретное предупреждение

```bat
findstr /I /C:"Mockito is currently" /C:"dynamically" /C:"Byte Buddy" /C:"Sharing is only" test-debug.txt
```

Назначение:

> Отделить Mockito warning от остальных предупреждений JVM.

---

## Посмотреть содержимое JAR

```bat
jar tf mockito-core-5.23.0.jar
```

Назначение:

> Проверить, какие классы и ресурсы находятся внутри JAR.

---

## Найти конкретные классы

```bat
jar tf mockito-core-5.23.0.jar | findstr /I "MockMaker"
```

Назначение:

> Найти классы, связанные с механизмом mock maker.

---

## Исследовать байткод

```bat
javap -private -c -classpath mockito-core-5.23.0.jar <CLASS>
```

Назначение:

> Посмотреть, какие методы реально вызываются внутри класса.

---

## Посмотреть manifest

```bat
jar xf mockito-core-5.23.0.jar META-INF/MANIFEST.MF
type META-INF\MANIFEST.MF
```

Назначение:

> Проверить, объявлен ли JAR Java Agent и какой класс является `Premain-Class`.

---

# 28. Универсальная методика для похожих проблем

Эту методику можно применять не только к Mockito.

## Шаг 1. Зафиксировать warning

Не начинать с изменения конфигурации.

Сначала записать:

```text
Что именно предупреждается?
Кто выводит warning?
При каком запуске?
```

---

## Шаг 2. Разделить warning и error

Например:

```text
WARNING
```

не означает:

```text
BUILD FAILURE
```

Сначала определить, действительно ли проблема влияет на выполнение.

---

## Шаг 3. Найти источник warning

Искать:

```text
Mockito
Byte Buddy
JUnit
JVM
Maven
```

в зависимости от текста сообщения.

---

## Шаг 4. Найти реальный механизм

Не ограничиваться:

> «Mockito требует agent».

Нужно установить:

```text
какой класс
 ↓
какой метод
 ↓
какая библиотека
 ↓
какой механизм JVM
```

В нашем случае:

```text
InlineDelegateByteBuddyMockMaker
        ↓
PremainAttachAccess
        ↓
ByteBuddyAgent.install()
        ↓
dynamic attach
```

---

## Шаг 5. Проверить, существует ли правильный механизм

В нашем случае:

```text
PremainAttach
```

и:

```text
Premain-Class
```

показали, что Mockito уже поддерживает правильный способ.

---

## Шаг 6. Проверить фактический runtime

Это очень важный принцип.

Не достаточно увидеть:

```xml
<argLine>-javaagent:...</argLine>
```

в `pom.xml`.

Нужно проверить:

```text
Forking command line
```

и убедиться, что JVM действительно получила:

```text
-javaagent:...
```

---

## Шаг 7. Проверить результат

После изменения:

```text
warning исчез?
тест проходит?
остались ли другие warning?
```

---

## Шаг 8. Не путать разные предупреждения

Если после исправления:

```text
WARNING A исчез
WARNING B остался
```

нужно считать это хорошим результатом.

Это означает:

> Причина WARNING A действительно была найдена и устранена.

---

# 29. Главные выводы

### Вывод №1

Mockito warning не был случайным сообщением.

Mockito действительно выполнял:

```text
dynamic attach
```

для получения instrumentation.

---

### Вывод №2

Mockito 5.23.0 уже содержит Java Agent.

Отдельный agent JAR искать не нужно.

Используется:

```text
mockito-core-5.23.0.jar
```

с:

```text
Premain-Class: org.mockito.internal.PremainAttach
```

---

### Вывод №3

Правильное решение — передать Mockito JVM заранее:

```text
-javaagent:mockito-core-5.23.0.jar
```

а не разрешать Mockito выполнять dynamic attach.

---

### Вывод №4

Maven/Surefire в проекте действительно передаёт Agent.

Это доказано не только содержимым `pom.xml`, но и фактической командой:

```text
Forking command line:
...
java -javaagent:...\mockito-core-5.23.0.jar
...
```

---

### Вывод №5

После исправления:

```text
Mockito is currently self-attaching
```

исчезло при:

```bat
mvn test
```

---

### Вывод №6

Оставшееся:

```text
Sharing is only supported for boot loader classes...
```

— отдельное предупреждение JVM.

Оно не является доказательством того, что Mockito снова делает dynamic attach.

---

### Вывод №7

Запуск теста через Maven и запуск теста из IntelliJ IDEA — это потенциально разные runtime-сценарии.

Поэтому Maven-настройка Surefire не обязана автоматически влиять на JUnit-запуск из IDEA.

---

# 30. Самый важный практический урок

При диагностике подобных проблем полезно двигаться не от:

```text
«Как убрать warning?»
```

а от:

```text
«Почему JVM вообще делает то, на что она предупреждает?» 
```

В нашем случае такой подход позволил перейти от:

```text
WARNING
```

к:

```text
Mockito
 ↓
Instrumentation
 ↓
PremainAttachAccess
 ↓
ByteBuddyAgent.install()
 ↓
dynamic attach
```

а затем найти правильный путь:

```text
Mockito JAR
 ↓
Premain-Class
 ↓
-javaagent
 ↓
JVM
 ↓
Mockito получает Instrumentation заранее
```

Это уже не просто «убрали предупреждение», а **поняли механизм работы Java Agent и причину поведения Mockito на современной JDK**.