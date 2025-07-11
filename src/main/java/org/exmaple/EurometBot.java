package org.exmaple;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.*;

public class EurometBot extends TelegramLongPollingBot {

    private final Map<Long, String> userState = new HashMap<>();
    private final Map<Long, Map<String, String>> userForm = new HashMap<>();

    private static final List<String> MANAGER_IDS = Arrays.asList("your id", "your id");

    @Override
    public String getBotToken() {
        return "Your token";
    }

    @Override
    public String getBotUsername() {
        return "EurometMykolaivBot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.getNewChatMembers() != null && !message.getNewChatMembers().isEmpty()) {
                handleNewChatMembers(message);
                return;
            }
            if (message.hasText()) {
                handleMessage(message);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallback(update);
        }
    }

    private void handleNewChatMembers(Message message) {
        Long chatId = message.getChatId();
        message.getNewChatMembers().forEach(user -> {
            String firstName = user.getFirstName() != null ? user.getFirstName() : "👤 Користувачу";
            String welcomeText = "👋 Привіт, *" + firstName + "*!\n\n" +
                    "Вас щойно додано до групи *ТОВ Євромет-Миколаїв* 🛫\n" ;

            sendMessage(chatId, welcomeText);
            sendWelcome(chatId); // запуск анкети
        });
    }

    private void handleMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();

        if ("/start".equals(text)) {
            sendWelcome(chatId);
            return;
        }

        if (userState.containsKey(chatId)) {
            String state = userState.get(chatId);

            if (state.equals("contact_message")) {
                for (String id : MANAGER_IDS) {
                    String msg = "📞 Звернення від клієнта:\n\n" +
                            "💬 " + text + "\n\n" +
                            "🔗 Telegram: [перейти](tg://user?id=" + chatId + ")";
                    sendMessage(Long.valueOf(id), msg);
                }
                sendMessage(chatId, "✅ Ваше повідомлення надіслано менеджеру. Очікуйте на відповідь.");
                userState.remove(chatId);
                return;
            }

            if (state.startsWith("replying_to_")) {
                String targetId = state.replace("replying_to_", "");
                sendMessage(Long.valueOf(targetId), "📩 *Повідомлення від менеджера:*\n\n" + text);
                sendMessage(chatId, "✅ Відповідь надіслано клієнту.");
                userState.remove(chatId);
                return;
            }

            Map<String, String> form = userForm.getOrDefault(chatId, new LinkedHashMap<>());

            switch (state) {
                case "sale_company": case "cost_company":
                    form.put("🏢 Назва підприємства", text);
                    sendNext(chatId, "📋 Якщо памʼятаєте, вкажіть код ЄДРПОУ:", state.replace("company", "code"));
                    break;
                case "sale_code": case "cost_code":
                    form.put("📋 Код ЄДРПОУ", text);
                    sendNext(chatId, "📍 Адреса завантаження:", state.replace("code", "address"));
                    break;
                case "sale_address": case "cost_address":
                    form.put("📍 Адреса завантаження", text);
                    sendCropOptions(chatId);
                    userState.put(chatId, state.replace("address", "crop"));
                    break;
                case "sale_volume": case "cost_volume":
                    form.put("📈 Обʼєм (т)", text);
                    sendNext(chatId, "🌾 Рік урожаю:", state.replace("volume", "year"));
                    break;
                case "sale_year": case "cost_year":
                    form.put("🌾 Рік урожаю", text);
                    sendDeliveryOptions(chatId);
                    userState.put(chatId, state.replace("year", "delivery"));
                    break;
                case "sale_comment": case "cost_comment":
                    form.put("📝 Коментар", text);
                    sendNext(chatId, "👨‍💼 ПІБ відповідальної особи:", state.replace("comment", "name"));
                    break;
                case "sale_name": case "cost_name":
                    form.put("👨‍💼 ПІБ", text);
                    sendNext(chatId, "☎️ Номер телефону:", state.replace("name", "phone"));
                    break;
                case "sale_phone": case "cost_phone":
                    form.put("☎️ Телефон", text);
                    sendFormResult(chatId, form);
                    break;
            }
            userForm.put(chatId, form);
        }
    }

    private void handleCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (data.startsWith("reply_to_")) {
            String targetId = data.replace("reply_to_", "");
            userState.put(chatId, "replying_to_" + targetId);
            sendMessage(chatId, "✍️ Введіть відповідь для клієнта:");
            return;
        }

        switch (data) {
            case "sale":
                userState.put(chatId, "sale_company");
                userForm.put(chatId, new LinkedHashMap<>());
                sendMessage(chatId, "📋 Розпочнемо анкету для *продажу*.\n\n✍️ Введіть, будь ласка, *назву підприємства*:");
                break;
            case "cost":
                userState.put(chatId, "cost_company");
                userForm.put(chatId, new LinkedHashMap<>());
                sendMessage(chatId, "💰 Розпочнемо анкету для *прорахунку вартості*.\n\n✍️ Введіть, будь ласка, *назву підприємства*:");
                break;
            case "contact":
                InlineKeyboardMarkup contactMarkup = InlineKeyboardMarkup.builder().keyboard(Arrays.asList(
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

                sendMessageWithMarkup(chatId,
                        "Звʼяжіться з менеджером *Вікторія* одним зі способів нижче:", contactMarkup);
                break;

            case "delivery_place":
                completeDelivery(chatId, "Продаж з місця");
                break;
            case "delivery_port":
                completeDelivery(chatId, "Доставка в порт");
                break;
            default:
                if (data.startsWith("crop_")) {
                    String crop = data.substring(5).replace("_", " ");
                    Map<String, String> form = userForm.getOrDefault(chatId, new LinkedHashMap<>());
                    form.put("Культура", crop);
                    userForm.put(chatId, form);
                    String current = userState.get(chatId);
                    userState.put(chatId, current.replace("crop", "volume"));
                    sendMessage(chatId, "Обʼєм (у тоннах):");
                }
                break;
        }
    }

    private void completeDelivery(Long chatId, String option) {
        Map<String, String> form = userForm.getOrDefault(chatId, new LinkedHashMap<>());
        form.put("Тип доставки", option);
        userForm.put(chatId, form);
        String current = userState.get(chatId);
        userState.put(chatId, current.replace("delivery", "comment"));
        sendMessage(chatId, "Коментар (за потреби):");
    }

    private void sendDeliveryOptions(Long chatId) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(Arrays.asList(
                Arrays.asList(
                        InlineKeyboardButton.builder().text("🏠 Продаж з місця").callbackData("delivery_place").build(),
                        InlineKeyboardButton.builder().text("⚓ Доставка в порт").callbackData("delivery_port").build()
                )
        )).build();

        sendMessageWithMarkup(chatId, "Оберіть тип доставки:", markup);
    }

    private void sendCropOptions(Long chatId) {
        String[][] crops = {
                {"🌾 Пшениця 2 Кл", "🌾 Пшениця 3 Кл"},
                {"🌾 Пшениця 4 Кл", "🌿 Ячмінь"},
                {"🌽 Кукурудза", "🥘 Соя ГМО"},
                {"🥘 Соя без ГМО", "🔠 Ріпак ГМО"},
                {"🔠 Ріпак без ГМО", "🟢 Горох"},
                {"🌛 Соняшник"}
        };

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (String[] row : crops) {
            List<InlineKeyboardButton> btns = new ArrayList<>();
            for (String item : row) {
                btns.add(InlineKeyboardButton.builder().text(item).callbackData("crop_" + item).build());
            }
            rows.add(btns);
        }

        sendMessageWithMarkup(chatId, "🌾 *Оберіть культуру:*", InlineKeyboardMarkup.builder().keyboard(rows).build());
    }

    private void sendWelcome(Long chatId) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(Arrays.asList(
                Arrays.asList(InlineKeyboardButton.builder().text("📜 Продаж").callbackData("sale").build()),
                Arrays.asList(InlineKeyboardButton.builder().text("💰 Прорахунок вартості").callbackData("cost").build()),
                Arrays.asList(InlineKeyboardButton.builder().text("📞 Зв'язок з менеджером").callbackData("contact").build())
        )).build();

        sendMessageWithMarkup(chatId,
                "👋 Вас вітає ТОВ *Євромет-Миколаїв*!\n" +
                        "👩‍💼 Менеджер: *Вікторія*\n\n" +
                        "🔽 Оберіть, будь ласка, одну з дій:", markup);
    }

    private void sendFormResult(Long chatId, Map<String, String> form) {
        StringBuilder sb = new StringBuilder("Надіслані дані:\n\n");
        form.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));

        sendMessage(chatId, "Дякуємо! Дані передано менеджеру ✅");
        for (String id : MANAGER_IDS) {
            InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(Arrays.asList(
                    Arrays.asList(InlineKeyboardButton.builder()
                            .text("✍️ Відповісти клієнту")
                            .callbackData("reply_to_" + chatId).build())
            )).build();

            sendMessageWithMarkup(Long.valueOf(id), "📩 Нова заявка з бота:\n\n" + sb, markup);
        }

        userState.remove(chatId);
        userForm.remove(chatId);
    }

    private void sendNext(Long chatId, String question, String nextState) {
        userState.put(chatId, nextState);
        sendMessage(chatId, question);
    }

    private void sendMessage(Long chatId, String text) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new EurometBot());
    }
}
