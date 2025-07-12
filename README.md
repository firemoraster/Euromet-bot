[![Java](https://img.shields.io/badge/Java-21-blue?style=flat-square&logo=openjdk&logoColor=white)](https://jdk.java.net/21/)
[![Telegram Bot API](https://img.shields.io/badge/Telegram_Bot_API-6.7.0-blue?style=flat-square&logo=telegram)](https://core.telegram.org/bots/api)
[![Build](https://img.shields.io/badge/Build-Maven_3.9.5-orange?style=flat-square&logo=apache-maven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](https://opensource.org/licenses/MIT)
[![Code Style](https://img.shields.io/badge/Code%20Style-Google%20Java%20Format-blueviolet?style=flat-square)](https://github.com/google/google-java-format)

# 🌾 Euromet Mykolaiv Telegram Bot

**Euromet Bot** - офіційний Telegram-бот ТОВ "Євромет-Миколаїв" для автоматизації роботи з клієнтами у сфері агробізнесу. Бот інтегрований в групи компанії та надає зручний інтерфейс для клієнтів через інтерактивні меню.

---

## 🌟 Особливості бота

### 📌 Групові можливості
- **Автоматичне вітання** нових учасників групи
- **Інтерактивні кнопки** для переходу в приватний чат з ботом
- **Гнучка система команд** для роботи як в групах, так і в приватних чатах

### 📋 Основні функції для клієнтів
- **📜 Анкета для продажу зерна** з усіма необхідними полями
- **💰 Розрахунок вартості** доставки та інших послуг
- **📞 Прямий зв'язок** з менеджером компанії
- **🔄 Збереження стану** під час заповнення форм

### 👨‍💼 Менеджерські функції
- **🔔 Автоматичні сповіщення** про нові заявки
- **✍️ Швидкі відповіді** клієнтам через інлайн-кнопки
- **📊 Структурований вивід** даних клієнтів

---

## 🛠 Технологічний стек

- **Java 21** - основна мова програмування
- **TelegramBots API 6.7.0** - для роботи з Telegram API
- **Maven 3.9.5** - система збірки
- **Інтерактивні клавіатури** - ReplyKeyboardMarkup та InlineKeyboardMarkup
- **Markdown-форматування** - для кращого відображення повідомлень

---

## 🖼 Інтерфейс бота

### Груповий інтерфейс
<img width="643" height="248" alt="image" src="https://github.com/user-attachments/assets/9c2b57b7-a439-403e-b913-ce61ebcffc55" />


> Користувачі в групі отримують інтерактивні кнопки для переходу в приватний чат

### Приватний чат
<img width="975" height="681" alt="image" src="https://github.com/user-attachments/assets/f4b21aa5-2fa6-4dfa-aa97-e9fbed9f1758" />


> Головне меню з нижньою клавіатурою та приклад заповненої анкети

### Менеджерський інтерфейс
<img width="1232" height="550" alt="image" src="https://github.com/user-attachments/assets/17a013d2-5f58-4196-931d-6bac48cd6381" />


> Автоматичне сповіщення менеджера з кнопками швидкої відповіді

---

## ⚙️ Архітектура бота

### Стани користувача
```mermaid
stateDiagram-v2
    [*] --> MainMenu
    MainMenu --> SaleForm: "Продаж зерна"
    MainMenu --> CostForm: "Розрахунок вартості"
    MainMenu --> ContactManager: "Зв'язок з менеджером"
    
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

## 🏗 Ключові класи

### `EurometBot`
Головний клас бота, успадкований від `TelegramLongPollingBot`. Відповідає за:
- Обробку вхідних повідомлень
- Управління станами користувачів
- Взаємодію з Telegram API

### `userState`
```java
private final Map<Long, String> userState = new HashMap<>();
