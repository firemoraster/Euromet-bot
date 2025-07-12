package org.exmaple;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EurometBot extends TelegramLongPollingBot {

    private final Map<Long, String> userState = new HashMap<>();
    private final Map<Long, Map<String, String>> userForm = new HashMap<>();
    private static final List<String> MANAGER_IDS = Arrays.asList("manager_id", "manager_id");

    @Override
    public String getBotToken() {
        return "Your token";
    }

    @Override
    public String getBotUsername() {
        return "EurometManager_Bot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message msg = update.getMessage();
            Long chatId = msg.getChatId();

            // --- ПОЧАТОК ЛОГУВАННЯ ДЛЯ ДІАГНОСТИКИ ---
            System.out.println("------------------------------------");
            System.out.println("Отримано повідомлення:");
            System.out.println("Chat ID: " + chatId);
            System.out.println("Text: " + msg.getText());
            System.out.println("Is User Message (приватний чат): " + msg.isUserMessage());
            System.out.println("Is Group Message: " + msg.isGroupMessage());
            System.out.println("Is SuperGroup Message: " + msg.isSuperGroupMessage());
            System.out.println("Has New Chat Members: " + (msg.getNewChatMembers() != null && !msg.getNewChatMembers().isEmpty()));
            // --- КІНЕЦЬ ЛОГУВАННЯ ДЛЯ ДІАГНОСТИКИ ---

            // 1) Обробка додавання нових учасників до ГРУПИ (включно з ботом)
            if (msg.getNewChatMembers() != null && !msg.getNewChatMembers().isEmpty()) {
                System.out.println("-> Виявлено нових учасників чату. Обробка вітання групи.");
                msg.getNewChatMembers().forEach(user -> {
                    try {
                        sendGroupWelcome(chatId); // Надсилаємо групове вітальне меню з inline кнопками
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                });
                System.out.println("<- Завершено обробку нових учасників.");
                return;
            }

            // 2) Обробка команди /start
            if (msg.hasText() && msg.getText().startsWith("/start")) {
                System.out.println("-> Виявлено команду /start.");
                if (msg.isGroupMessage() || msg.isSuperGroupMessage()) {
                    System.out.println("--> Це команда /start у ГРУПОВОМУ чаті.");
                    String botUsernameWithAt = "@" + getBotUsername();
                    if (msg.getText().equals("/start") || msg.getText().equals("/start" + botUsernameWithAt)) {
                        System.out.println("--> Надсилаємо групове вітальне меню (sendGroupWelcome) в групу.");
                        try {
                            sendGroupWelcome(chatId);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        System.out.println("--> Це deep link /start у групі. Ігноруємо.");
                    }
                    System.out.println("<- Завершено обробку групового /start.");
                    return;
                } else if (msg.isUserMessage()) {
                    System.out.println("--> Це команда /start у ПРИВАТНОМУ чаті. Передаємо в handlePrivateMessage.");
                    handlePrivateMessage(msg);
                    System.out.println("<- Завершено обробку приватного /start.");
                    return;
                }
            }

            // 3) Обробка інших повідомлень.
            if (msg.isUserMessage()) {
                System.out.println("-> Це інше повідомлення в ПРИВАТНОМУ чаті. Передаємо в handlePrivateMessage.");
                handlePrivateMessage(msg);
                System.out.println("<- Завершено обробку іншого приватного повідомлення.");
            } else {
                System.out.println("-> Ігноруємо повідомлення в груповому чаті (не /start і не новий учасник).");
            }
            System.out.println("------------------------------------");
        } else if (update.hasCallbackQuery()) {
            System.out.println("------------------------------------");
            System.out.println("Отримано CallbackQuery. Передаємо в handleCallback.");
            handleCallback(update);
            System.out.println("------------------------------------");
        }
    }

    // Метод для надсилання вітального повідомлення в ГРУПОВИЙ чат з inline кнопками
    private void sendGroupWelcome(Long chatId) throws TelegramApiException {
        System.out.println("--- sendGroupWelcome викликано для chatId: " + chatId + " ---");
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(Arrays.asList(
                Arrays.asList(
                        InlineKeyboardButton.builder()
                                .text("📜 Продаж зерна")
                                .url("https://t.me/" + getBotUsername() + "?start=sale")
                                .build()
                ),
                Arrays.asList(
                        InlineKeyboardButton.builder()
                                .text("💰 Розрахунок вартості")
                                .url("https://t.me/" + getBotUsername() + "?start=cost")
                                .build()
                ),
                Arrays.asList(
                        InlineKeyboardButton.builder()
                                .text("📞 Зв'язок з менеджером")
                                .url("https://t.me/" + getBotUsername() + "?start=contact")
                                .build()
                )
        )).build();

        execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text("👋 *Вас вітає Євромет-Миколаїв!*\n\n" +
                        "Натисніть одну з кнопок нижче, щоб перейти до приватного чату з ботом і продовжити:")
                .replyMarkup(markup)
                .parseMode("Markdown")
                .build()
        );
        System.out.println("--- sendGroupWelcome завершено. ---");
    }

    // Метод для надсилання вітального повідомлення в ГРУПУ, коли приєднується користувач
    private void sendUserWelcome(Long chatId, org.telegram.telegrambots.meta.api.objects.User user) throws TelegramApiException {
        System.out.println("--- sendUserWelcome викликано для chatId: " + chatId + " (користувач: " + user.getFirstName() + ") ---");
        sendGroupWelcome(chatId); // Надсилаємо вітальне меню з inline кнопками в групу при приєднанні користувача
        System.out.println("--- sendUserWelcome завершено. ---");
    }

    // Метод для обробки повідомлень у ПРИВАТНОМУ чаті
    private void handlePrivateMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();
        System.out.println("--- handlePrivateMessage викликано для chatId: " + chatId + ", текст: '" + text + "' ---");

        // Обробка команди /start у приватному чаті
        if (text.startsWith("/start")) {
            System.out.println("-> Обробка /start в приватному чаті.");
            String[] parts = text.split(" ");
            String startParam = null;
            if (parts.length > 1) { // Це /start з параметром (глибоке посилання), наприклад /start sale
                startParam = parts[1];
                System.out.println("--> /start з параметром: " + startParam);
            } else { // Це просто /start без параметрів (користувач сам набрав /start)
                System.out.println("--> Просто /start.");
            }

            // Завжди показуємо головне меню з ReplyKeyboardMarkup
            showMainMenu(chatId);

            // Якщо був параметр, запускаємо відповідну дію після показу головного меню
            if (startParam != null) {
                switch (startParam) {
                    case "sale":
                        startSaleForm(chatId);
                        break;
                    case "cost":
                        startCostForm(chatId);
                        break;
                    case "contact":
                        showContactOptions(chatId);
                        break;
                    default:
                        System.out.println("--> Невідомий параметр /start. Головне меню вже показано.");
                }
            }
            System.out.println("<- Завершено обробку /start в приватному чаті.");
            return;
        }

        // Обробка натискань на кнопки ReplyKeyboardMarkup (нижні кнопки)
        if ("📜 Продаж зерна".equals(text)) {
            System.out.println("-> Натиснута кнопка 'Продаж зерна'.");
            startSaleForm(chatId);
            return;
        }
        if ("💰 Розрахунок вартості".equals(text)) {
            System.out.println("-> Натиснута кнопка 'Розрахунок вартості'.");
            startCostForm(chatId);
            return;
        }
        if ("📞 Зв'язок з менеджером".equals(text)) {
            System.out.println("-> Натиснута кнопка 'Зв'язок з менеджером'.");
            showContactOptions(chatId);
            return;
        }

        // Обробка станів: анкета, повідомлення менеджеру тощо
        if (userState.containsKey(chatId)) {
            String state = userState.get(chatId);
            System.out.println("-> Обробка стану користувача: " + state);

            if ("contact_message".equals(state)) {
                MANAGER_IDS.forEach(id -> {
                    String msg = "📞 Звернення від клієнта:\n\n💬 " + text;
                    InlineKeyboardMarkup m = InlineKeyboardMarkup.builder().keyboard(Arrays.asList(
                            Arrays.asList(
                                    InlineKeyboardButton.builder()
                                            .text("✍️ Відповісти через бота")
                                            .callbackData("reply_to_" + chatId).build(),
                                    InlineKeyboardButton.builder()
                                            .text("🔗 Перейти в Telegram")
                                            .url("tg://user?id=" + chatId).build()
                            )
                    )).build();
                    sendMessageWithMarkup(Long.valueOf(id), msg, m);
                });
                sendMessage(chatId, "✅ Ваше повідомлення надіслано менеджеру. Очікуйте на відповідь.");
                userState.remove(chatId);
                showMainMenu(chatId);
                return;
            }

            if (state.startsWith("replying_to_")) {
                String targetId = state.substring("replying_to_".length());
                sendMessage(Long.valueOf(targetId), "📩 *Від менеджера:*\n\n" + text);
                sendMessage(chatId, "✅ Відповідь надіслано клієнту.");
                userState.remove(chatId);
                showMainMenu(chatId);
                return;
            }

            // Анкета (sale_* / cost_*)
            Map<String, String> form = userForm.getOrDefault(chatId, new LinkedHashMap<>());
            switch (state) {
                case "sale_company":
                case "cost_company":
                    form.put("🏢 Назва підприємства", text);
                    sendNext(chatId, "📋 Якщо пам’ятаєте, вкажіть код ЄДРПОУ:", state.replace("company", "code"));
                    break;
                case "sale_code":
                case "cost_code":
                    form.put("📋 Код ЄДРПОУ", text);
                    sendNext(chatId, "📍 Адреса завантаження:", state.replace("code", "address"));
                    break;
                case "sale_address":
                case "cost_address":
                    form.put("📍 Адреса завантаження", text);
                    sendCropOptions(chatId);
                    userState.put(chatId, state.replace("address", "crop"));
                    break;
                case "sale_volume":
                case "cost_volume":
                    form.put("📈 Об’єм (т)", text);
                    sendNext(chatId, "🌾 Рік урожаю:", state.replace("volume", "year"));
                    break;
                case "sale_year":
                case "cost_year":
                    form.put("🌾 Рік урожаю", text);
                    sendDeliveryOptions(chatId);
                    userState.put(chatId, state.replace("year", "delivery"));
                    break;
                case "sale_comment":
                case "cost_comment":
                    form.put("📝 Коментар", text);
                    sendNext(chatId, "👨‍💼 ПІБ відповідальної особи:", state.replace("comment", "name"));
                    break;
                case "sale_name":
                case "cost_name":
                    form.put("👨‍💼 ПІБ", text);
                    sendNext(chatId, "☎️ Телефон:", state.replace("name", "phone"));
                    break;
                case "sale_phone":
                case "cost_phone":
                    form.put("☎️ Телефон", text);
                    sendFormResult(chatId, form);
                    break;
            }
            userForm.put(chatId, form);
        } else {
            System.out.println("-> Невідоме повідомлення або неактивний стан. Показуємо головне меню.");
            showMainMenu(chatId);
        }
        System.out.println("--- handlePrivateMessage завершено. ---");
    }

    // Метод для обробки натискань на Inline кнопки (які приходять як CallbackQuery)
    private void handleCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        System.out.println("--- handleCallback викликано для chatId: " + chatId + ", дані: '" + data + "' ---");

        if ("reply_to_manager".equals(data)) {
            System.out.println("-> Callback: reply_to_manager.");
            userState.put(chatId, "contact_message");
            sendMessage(chatId, "✍️ Введіть Ваше повідомлення для менеджера:");
            return;
        }

        if (data.startsWith("reply_to_")) {
            String targetId = data.replace("reply_to_", "");
            if (targetId.matches("\\d+")) {
                System.out.println("-> Callback: replying to client " + targetId);
                userState.put(chatId, "replying_to_" + targetId);
                sendMessage(chatId, "✍️ Введіть відповідь для клієнта:");
                return;
            }
        }

        switch (data) {
            case "sale":
                System.out.println("-> Callback: sale. Запускаємо форму продажу.");
                // Тут ми вже знаходимося в приватному чаті, меню знизу вже має бути
                startSaleForm(chatId);
                break;
            case "cost":
                System.out.println("-> Callback: cost. Запускаємо форму вартості.");
                startCostForm(chatId);
                break;
            case "contact":
                System.out.println("-> Callback: contact. Показуємо опції контакту.");
                showContactOptions(chatId);
                break;
            case "delivery_place":
                System.out.println("-> Callback: delivery_place.");
                completeDelivery(chatId, "Продаж з місця");
                break;
            case "delivery_port":
                System.out.println("-> Callback: delivery_port.");
                completeDelivery(chatId, "Доставка в порт");
                break;
            default:
                if (data.startsWith("crop_")) {
                    System.out.println("-> Callback: crop selection.");
                    String crop = data.substring(5).replace("_", " ");
                    Map<String, String> form = userForm.getOrDefault(chatId, new LinkedHashMap<>());
                    form.put("Культура", crop);
                    userForm.put(chatId, form);
                    String current = userState.get(chatId);
                    userState.put(chatId, current.replace("crop", "volume"));
                    sendMessage(chatId, "Обʼєм (у тоннах):");
                } else {
                    System.out.println("-> Callback: Unknown data.");
                }
                break;
        }
        System.out.println("--- handleCallback завершено. ---");
    }

    // Метод для відображення головного меню з ReplyKeyboardMarkup (нижні кнопки)
    private void showMainMenu(Long chatId) {
        System.out.println("--- showMainMenu викликано для chatId: " + chatId + " ---");
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📜 Продаж зерна"));
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("💰 Розрахунок вартості"));
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("📞 Зв'язок з менеджером"));
        keyboard.add(row3);

        replyKeyboardMarkup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("👋 Вас вітає *Євромет-Миколаїв*!\n👩‍💼 Менеджер: *Вікторія*\n\nОберіть потрібну дію:");
        message.setReplyMarkup(replyKeyboardMarkup);
        message.setParseMode("Markdown");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        System.out.println("--- showMainMenu завершено. ---");
    }

    private void startSaleForm(Long chatId) {
        System.out.println("--- startSaleForm викликано для chatId: " + chatId + " ---");
        userState.put(chatId, "sale_company");
        userForm.put(chatId, new LinkedHashMap<>());
        sendMessage(chatId, "📋 Розпочнемо анкету для *продажу зерна*.\n\n✍️ Введіть назву вашого підприємства:");
        System.out.println("--- startSaleForm завершено. ---");
    }

    private void startCostForm(Long chatId) {
        System.out.println("--- startCostForm викликано для chatId: " + chatId + " ---");
        userState.put(chatId, "cost_company");
        userForm.put(chatId, new LinkedHashMap<>());
        sendMessage(chatId, "💰 Розпочнемо анкету для *розрахунку вартості*.\n\n✍️ Введіть назву вашого підприємства:");
        System.out.println("--- startCostForm завершено. ---");
    }

    private void showContactOptions(Long chatId) {
        System.out.println("--- showContactOptions викликано для chatId: " + chatId + " ---");
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(Arrays.asList(
                Arrays.asList(
                        InlineKeyboardButton.builder()
                                .text("📩 Написати через бота")
                                .callbackData("reply_to_manager").build()
                ),
                Arrays.asList(
                        InlineKeyboardButton.builder()
                                .text("💬 Написати напряму в Telegram")
                                .url("https://t.me/ArkhypenkoV").build()
                )
        )).build();

        sendMessageWithMarkup(chatId, "Звʼяжіться з менеджером *Вікторія* одним зі способів:", markup);
        System.out.println("--- showContactOptions завершено. ---");
    }

    private void completeDelivery(Long chatId, String option) {
        System.out.println("--- completeDelivery викликано для chatId: " + chatId + ", опція: " + option + " ---");
        Map<String, String> form = userForm.getOrDefault(chatId, new LinkedHashMap<>());
        form.put("Тип доставки", option);
        userForm.put(chatId, form);
        String current = userState.get(chatId);
        userState.put(chatId, current.replace("delivery", "comment"));
        sendMessage(chatId, "📝 Введіть додатковий коментар (якщо потрібно):");
        System.out.println("--- completeDelivery завершено. ---");
    }

    private void sendCropOptions(Long chatId) {
        System.out.println("--- sendCropOptions викликано для chatId: " + chatId + " ---");
        String[][] crops = {
                {"🌾 Пшениця 2 класу", "🌾 Пшениця 3 класу"},
                {"🌾 Пшениця 4 класу", "🌿 Ячмінь"},
                {"🌽 Кукурудза", "🥘 Соя ГМО"},
                {"🥘 Соя без ГМО", "🔠 Ріпак ГМО"},
                {"🔠 Ріпак без ГМО", "🟢 Горох"},
                {"🌻 Соняшник"}
        };

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (String[] row : crops) {
            List<InlineKeyboardButton> btns = new ArrayList<>();
            for (String item : row) {
                btns.add(InlineKeyboardButton.builder()
                        .text(item)
                        .callbackData("crop_" + item.replace(" ", "_")).build());
            }
            rows.add(btns);
        }

        sendMessageWithMarkup(chatId, "🌾 Оберіть культуру:",
                InlineKeyboardMarkup.builder().keyboard(rows).build());
        System.out.println("--- sendCropOptions завершено. ---");
    }

    private void sendDeliveryOptions(Long chatId) {
        System.out.println("--- sendDeliveryOptions викликано для chatId: " + chatId + " ---");
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(Arrays.asList(
                Arrays.asList(
                        InlineKeyboardButton.builder().text("🏠 Продаж з місця").callbackData("delivery_place").build(),
                        InlineKeyboardButton.builder().text("⚓ Доставка в порт").callbackData("delivery_port").build()
                )
        )).build();

        sendMessageWithMarkup(chatId, "🚛 Оберіть тип доставки:", markup);
        System.out.println("--- sendDeliveryOptions завершено. ---");
    }

    private void sendFormResult(Long chatId, Map<String, String> form) {
        System.out.println("--- sendFormResult викликано для chatId: " + chatId + " ---");
        StringBuilder sb = new StringBuilder("📄 *Дані заявки:*\n\n");
        form.forEach((k, v) -> sb.append("• ").append(k).append(": ").append(v).append("\n"));

        sendMessage(chatId, "✅ Дякуємо! Ваші дані передано менеджеру.");

        String state = userState.getOrDefault(chatId, "");
        String type = state.startsWith("cost_") ? "🔍 *Заявка на розрахунок вартості*" : "📜 *Заявка на продаж зерна*";
        String managerMessage = type + ":\n\n" + sb.toString();
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(Arrays.asList(
                Arrays.asList(
                        InlineKeyboardButton.builder()
                                .text("✍️ Відповісти клієнту")
                                .callbackData("reply_to_" + chatId).build(),
                        InlineKeyboardButton.builder()
                                .text("💬 Відкрити чат")
                                .url("tg://user?id=" + chatId).build()
                )
        )).build();

        MANAGER_IDS.forEach(managerId -> {
            sendMessageWithMarkup(Long.parseLong(managerId), managerMessage, markup);
        });

        userState.remove(chatId);
        userForm.remove(chatId);
        showMainMenu(chatId); // Тут showMainMenu залишається, бо анкета завершена
        System.out.println("--- sendFormResult завершено. ---");
    }

    private void sendNext(Long chatId, String question, String nextState) {
        System.out.println("--- sendNext викликано для chatId: " + chatId + ", наступний стан: " + nextState + " ---");
        userState.put(chatId, nextState);
        sendMessage(chatId, question);
        System.out.println("--- sendNext завершено. ---");
    }

    private void sendMessage(Long chatId, String text) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            String err = e.getMessage() == null ? "" : e.getMessage();
            // Якщо бот заблокований користувачем або не має права писати в цей чат – просто ігноруємо
            if (err.contains("Forbidden")) {
                // ── swallow silently ──
            } else {
                // Всі інші помилки – залогувати
                e.printStackTrace();
            }
        }
    }


    private void sendMessageWithMarkup(Long chatId, String text, InlineKeyboardMarkup markup) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .replyMarkup(markup)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            String err = e.getMessage() == null ? "" : e.getMessage();
            if (err.contains("Forbidden")) {
                // ── swallow silently ──
            } else {
                e.printStackTrace();
            }
        }
    }


    // Метод для екранування спеціальних символів MarkdownV2
    private String escapeMarkdown(String text) {
        String escapedText = text;
        escapedText = escapedText.replace("_", "\\_");
        escapedText = escapedText.replace("*", "\\*");
        escapedText = escapedText.replace("[", "\\[");
        escapedText = escapedText.replace("]", "\\]");
        escapedText = escapedText.replace("(", "\\(");
        escapedText = escapedText.replace(")", "\\)");
        escapedText = escapedText.replace("~", "\\~");
        escapedText = escapedText.replace("`", "\\`");
        escapedText = escapedText.replace(">", "\\>");
        escapedText = escapedText.replace("#", "\\#");
        escapedText = escapedText.replace("+", "\\+");
        escapedText = escapedText.replace("-", "\\-");
        escapedText = escapedText.replace("=", "\\=");
        escapedText = escapedText.replace("|", "\\|");
        escapedText = escapedText.replace("{", "\\{");
        escapedText = escapedText.replace("}", "\\}");
        escapedText = escapedText.replace(".", "\\.");
        escapedText = escapedText.replace("!", "\\!");
        return escapedText;
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new EurometBot());
    }
}
