import com.vdurmont.emoji.EmojiManager;
import org.jsoup.nodes.Document;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import com.vdurmont.emoji.EmojiParser;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class MouserLinkBot extends InputProcessing implements LongPollingSingleThreadUpdateConsumer {
    private static final String botToken = "5461739511:AAEA_MB6mVLBu3e9dpChEaDlEJKC7fYC1C0";
    private final TelegramClient telegramClient = new OkHttpTelegramClient(botToken);

    private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(
            10, // corePoolSize
            50, // maximumPoolSize
            60L, TimeUnit.SECONDS, // keepAliveTime
            new SynchronousQueue<>() // очередь задач
    );

    private final Map<Long, Future<?>> activeTasks = new ConcurrentHashMap<>();

    public MouserLinkBot() {
    }

    public String getBotToken() {
        return botToken;
    }


    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.contains("/start")) {
                executorService.submit(() -> start(chatId));
            } else if (messageText.contains("/help")) {
                executorService.submit(() -> help(chatId));
            } else if (messageText.contains("/stop")) {
                executorService.submit(() -> stop(chatId));
            } else if (EmojiManager.containsEmoji(messageText)) {
                executorService.submit(() -> sendWarningMessage(chatId));
            } else {
                Future<?> previousTask = activeTasks.get(chatId);
                if (previousTask != null && !previousTask.isDone()) {
                    previousTask.cancel(true);
                }

                Future<?> future = executorService.submit(() -> searchDetail(messageText, chatId));
                activeTasks.put(chatId, future);
            }
        } else {
                executorService.submit(() -> sendWarningMessage(update.getMessage().getChatId()));
            }
        }





    public void sendWarningMessage(long chatId) {
        sendMessage("<b>Ошибка при обработке URL:</b>\n<blockquote>Возможно введены некорректные данные (такое происходит при отправке стикеров, файлов, голосовых и т. д.)</blockquote>", chatId);
    }

    public void start(long chatId) {
        sendMessage("""
                <b>Привет, для поиска конденсаторов просто напиши сообщение в формате:
                "0805 X7R 0,1мкФ ±5% 50В".\s
                Если что-то не получается напиши /help</b>
                Для остановки поиска, во время выполнения можно написать /stop""", chatId);
    }

    private void stop(long chatId) {
        Future<?> future = activeTasks.get(chatId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            sendMessage("Текущая операция была остановлена.", chatId);
        } else {
            sendMessage("Нет активных операций для остановки.", chatId);
        }
    }

    public void help(long chatId) {
        sendMessage("""
                <b> Как нужно искать информацию:</b>
                1. Написать в чат характеристики конденсатора, например: <u> 0805 X7R 0,1мкФ ±5% 50В</u>
                <blockquote>
                • 0805 - это Case-сode(Кейс-код) - обычно набор цифр, не раделять точками и запятыми (или другими знаками)
                                                
                • X7R - это Dielectric(Диэлеткрик) - можно писать как угодно (заглавными, строчными)
                                                
                • 0,1мкФ - это Capacitance(Сопротивление) - можно писать как угодно (русский, английский), но лучше между цифрами и буквами не делать пробелы.
                                                
                • ±5% - это Tolerance(Допуск) - перед цифрой <b>ВСЕГДА</b> должен стоять символ ±
                                                
                • 50В - Voltage(Вольтаж) - строка <b>ВСЕГДА</b> должна заканчиваться Вольтажом, если ищите несколько конденсаторов - каждая строка (стопка характеристик) должна заканчиваться буквой "В"
                                                
                • Также есть ещё 1 принимаемый параметр: <b> "Корпус A"</b> - как только БОТ видит слово "корпус" он начинает поиск в разделе "Tantalum Capacitors" добавляя другие характеристики, которые
                вы написали и которые можно добавить к поиску. <b>ВАЖНО! Поиск кейса ведется на английском, поэтому буква после слова "Корпус" должна быть только на английском! То есть в "Корпус A" - А - это английская а </b>
                </blockquote>
                                                
                2. После ввода необходимых характеристик нажимайте Enter или кнопку отправить.
                                                
                3. Бот выдаст одну из нескольких реакций:
                                                
                <blockquote>
                • Если все введено корректно, то будет осуществляться поиск самого дешевого конденсатора (при заказе от 100 шт.) на найденной странице и это может занять чуть дольше времени, чем обычно отвечает бот.
                Не переживайте. Если все успешно, Вы получите сообщение с текстом "Найдено. Ссылка на страницу:" далее ссылка на страницу по которой производился поиск и характеристики самого дешевого конденсатора (при заказе от 100 шт.).
                А также под Part Number есть сам парт номер и если на него нажать - вы перейдете конкретно на страницу этого конденсатора.
                                                
                • Если бот ничего не нашел, Вы получите сообщение: "<s>Результаты не найдены!</s> Ссылка:" далее ссылка по которой бот пытался что-то найти.
                                                
                • В конце любого запроса вы получите сообщение: "Обработка завершена. Я смог найти: <b><u>i из j</u></b> запросов, где
                i - количество запросов, которое завершилось успешно, то есть "Найдено. Ссылка на страницу:"
                j - количество общих запросов, которое было сделано, то есть если Вы отправите 50 характеристик - там будет цифра 50.
                </blockquote>""", chatId);
    }

    public void searchDetail(String messageText, long chatId) {

        // Отправка стикера о начале выполнения операции (анимированный стикер)
        SendSticker loadBarSticker = SendSticker.builder()
                .chatId(chatId)
                .sticker(new InputFile("CAACAgIAAxkBAW2ipGaYouzsHIOTxbyUzhOvf0l8orhLAALuUwACStfBSA4yUivYezCaNQQ"))
                .build();
       /* SendMessage loadBarSticker = SendMessage.builder()
                .chatId(chatId)
                .text("⏳")
                .build();*/
        Message sentStickerMessage = null;
        try {
            sentStickerMessage = telegramClient.execute(loadBarSticker);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendMessage("Не удалось отправить сообщение о начале операции.", chatId);
            return;
        }

        try {
            // Проверка на прерывание в начале выполнения
            if (Thread.currentThread().isInterrupted()) {
                sendMessage("Текущая операция была прервана до начала выполнения.", chatId);
                deleteMessage(sentStickerMessage);
                return;
            }

            List<String> generatedUrls = this.processInputData(messageText);
            long iterations = 0;
            long completeRequests = 0;
            Parser parser = new Parser();
            String completeParsing = "";
            String noResultsText = "No Results Found. Try modifying your search term below or visit our Help Centre.";

            for (String url : generatedUrls) {
                // Регулярная проверка на прерывание в цикле
                if (Thread.currentThread().isInterrupted()) {
                    sendMessage("Текущая операция была остановлена.", chatId);
                    deleteMessage(sentStickerMessage);
                    return;
                }

                try {
                    Document doc = ConnectionURL.parsePage(url);

                    if (!doc.body().text().contains(noResultsText)) {
                        completeRequests++;
                        completeParsing = parser.parsingFoundElement(url);
                        sendMessage("<b>" + (++iterations) + ". Найдено. Ссылка на страницу: \n</b> " + url + completeParsing + "\n", chatId);
                    } else {
                        sendMessage("<b>" + (++iterations) + ". <s>Результаты не найдены!</s> Ссылка на страницу: \n</b>" + url + "\n<i>Попробуйте изменить параметры поиска или проверить не допустил ли я ошибку в генерации ссылки.</i>", chatId);
                    }
                } catch (InterruptedException e) {
                    // Ловим прерывание и корректно завершаем выполнение
                    sendMessage("Текущая операция была остановлена.", chatId);
                    deleteMessage(sentStickerMessage);
                    Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
                    return;
                } catch (Exception e) {
                    deleteMessage(sentStickerMessage);
                    throw new RuntimeException(e);
                }
            }

            sendMessage("Обработка завершена.\nЯ смог найти: <b><u>" + completeRequests + " из " + iterations + "</u></b> запросов", chatId);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Удаление стикера о выполнении операции
            deleteMessage(sentStickerMessage);
        }
    }

    private void deleteMessage(Message message) {
        if (message != null) {
            DeleteMessage deleteMessage = new DeleteMessage(message.getChatId().toString(), message.getMessageId());
            deleteMessage.setChatId(message.getChatId());
            deleteMessage.setMessageId(message.getMessageId());
            try {
                telegramClient.execute(deleteMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }


    private void sendMessage(String text, long chatId) {
        SendMessage message = SendMessage
                .builder()
                .parseMode("HTML")
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
