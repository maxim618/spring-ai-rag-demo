
const form = document.getElementById("question-form");
const imageInput = document.getElementById("image-input");
const submitButton = document.getElementById("submit-button");

const resultSection = document.getElementById("result");
const questionElement = document.getElementById("question");
const correctOptionsElement = document.getElementById("correct-options");
const confidenceElement = document.getElementById("match-confidence");
const statusElement = document.getElementById("status");

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const image = imageInput.files[0];

    if (!image) {
        showError("Выберите фотографию вопроса.");
        return;
    }

    const formData = new FormData();
    formData.append("image", image);

    submitButton.disabled = true;
    submitButton.textContent = "Обрабатываем...";

    statusElement.textContent =
        "Распознаём вопрос и ищем ответ...";

    statusElement.classList.remove("error");

    resultSection.hidden = true;
    questionElement.textContent = "";
    correctOptionsElement.replaceChildren();
    confidenceElement.textContent = "";

    try {
        const response = await fetch(
            "/checklist/questions/identify-screenshot",
            {
                method: "POST",
                body: formData
            }
        );

        const data = await response.json();

        if (!response.ok) {
            console.error("HTTP status:", response.status);
            console.error("Backend response:", data);

            const message = data.error
                || JSON.stringify(data, null, 2)
                || "Не удалось определить правильный ответ.";

            throw new Error(
                `HTTP ${response.status}: ${message}`
            );
        }

        const identification = data.identification;

        if (
            !identification
            || !identification.questionId
            || !identification.correctOptions
            || !identification.options
        ) {
            throw new Error(
                "Ответ сервера не содержит найденных правильных вариантов."
            );
        }

        // Каноническая формулировка вопроса из Knowledge Base.
        questionElement.textContent = identification.question;

        // Процент уверенности идентификации.
        const confidence = identification.confidence;

        if (confidence != null) {
            const percentage = Math.round(confidence * 100);

            confidenceElement.textContent =
                `Уверенность идентификации: ${percentage}%`;
        } else {
            confidenceElement.textContent =
                "Уверенность идентификации не указана";
        }

        // Идентификаторы правильных вариантов из Knowledge Base.
        const correctOptionIds = new Set(
            identification.correctOptions
        );

        // Канонические варианты ответа из Knowledge Base.
        const correctOptions = identification.options.filter(
            option => correctOptionIds.has(option.id)
        );

        if (correctOptions.length === 0) {
            throw new Error(
                "Не удалось получить тексты правильных вариантов."
            );
        }

        // Выводим полные тексты правильных вариантов.
        for (const option of correctOptions) {
            const listItem = document.createElement("li");

            listItem.textContent = option.text;

            correctOptionsElement.appendChild(listItem);
        }

        resultSection.hidden = false;

        statusElement.textContent =
            `Вопрос найден: ${identification.questionId}`;

    } catch (error) {
        showError(
            error.message || "Произошла ошибка при обработке."
        );
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = "Найти ответ";
    }
});

function showError(message) {
    statusElement.textContent = message;
    statusElement.classList.add("error");
}
