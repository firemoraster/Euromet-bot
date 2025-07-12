[![Java](https://img.shields.io/badge/Java-21-blue?style=flat-square&logo=openjdk&logoColor=white)](https://jdk.java.net/21/)
[![Telegram Bot API](https://img.shields.io/badge/Telegram_Bot_API-6.7.0-blue?style=flat-square&logo=telegram)](https://core.telegram.org/bots/api)
[![Build](https://img.shields.io/badge/Build-Maven_3.9.5-orange?style=flat-square&logo=apache-maven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](https://opensource.org/licenses/MIT)
[![Code Style](https://img.shields.io/badge/Code%20Style-Google%20Java%20Format-blueviolet?style=flat-square)](https://github.com/google/google-java-format)

# 🌾 Euromet Mykolaiv Telegram Bot

**Euromet Bot** — офіційний Telegram-бот ТОВ "Євромет-Миколаїв" для автоматизації роботи з клієнтами у сфері агробізнесу. Бот інтегрований у групи компанії та надає зручний інтерфейс через інтерактивні меню.

---

## 🌟 Основні функції

### 📌 Групові можливості
- Автоматичне привітання нових учасників
- Кнопки переходу до приватного чату
- Команди, які працюють у групах і приваті

### 📋 Для клієнтів
- 📜 Анкета для продажу зерна
- 💰 Розрахунок вартості доставки
- 📞 Прямий зв’язок з менеджером
- 🔄 Збереження стану під час заповнення

### 👨‍💼 Для менеджерів
- 🔔 Автосповіщення про нові заявки
- ✍️ Інлайн-відповіді клієнтам
- 📊 Структурований формат заявок

---

## 🛠 Технології

- **Java 21**
- **TelegramBots API 6.7.0**
- **Maven 3.9.5**
- **ReplyKeyboardMarkup**, **InlineKeyboardMarkup**
- **Markdown-повідомлення**

---

## 🖼 Інтерфейс бота

### Група
![Група](https://github.com/user-attachments/assets/9c2b57b7-a439-403e-b913-ce61ebcffc55)

> Інтерактивні кнопки переходу до чату

### Приват
![Приват](https://github.com/user-attachments/assets/f4b21aa5-2fa6-4dfa-aa97-e9fbed9f1758)

> Меню з клавіатурою і анкета

### Менеджер
![Менеджер](https://github.com/user-attachments/assets/17a013d2-5f58-4196-931d-6bac48cd6381)

> Повідомлення менеджеру з кнопками

---

## ⚙️ Архітектура: стани користувача

```mermaid
stateDiagram-v2
    [*] --> MainMenu
    MainMenu --> SaleForm : "Продаж зерна"
    MainMenu --> CostForm : "Розрахунок вартості"
    MainMenu --> ContactManager : "Зв’язок з менеджером"

    SaleForm --> CompanyName
    CompanyName --> EDRPOUCode
    EDRPOUCode --> LoadingAddress
    LoadingAddress --> CropSelection
    CropSelection --> VolumeInput
    VolumeInput --> HarvestYear
    HarvestYear --> DeliveryType
    DeliveryType --> Comments
    Comments --> ContactPerson
    ContactPerson --> PhoneNumber
    PhoneNumber --> FormSubmission

    CostForm --> CompanyName
    CompanyName --> DeliveryType
    DeliveryType --> CropSelection
    CropSelection --> VolumeInput
    VolumeInput --> HarvestYear
    HarvestYear --> Comments
    Comments --> ContactPerson
    ContactPerson --> PhoneNumber
    PhoneNumber --> CostSubmission
