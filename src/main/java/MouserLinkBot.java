import org.checkerframework.checker.units.qual.C;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MouserLinkBot extends InputProcessing implements LongPollingSingleThreadUpdateConsumer {
    private static final String botToken = "5461739511:AAEA_MB6mVLBu3e9dpChEaDlEJKC7fYC1C0";
    private final TelegramClient telegramClient = new OkHttpTelegramClient(botToken);
    private ExecutorService executorService;
    private Map<Long, Future<?>> activeTasks;

    public MouserLinkBot() {
        executorService = Executors.newCachedThreadPool();
        activeTasks = new HashMap<>();
    }

    public String getBotToken() {
        return botToken;
    }


    @Override
    public void consume(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            String message_text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();

            if (message_text.contains("/start")) {
                executorService.submit(() -> start(chat_id));
            } else if (message_text.contains("/help")) {
                executorService.submit(() -> help(chat_id));
            } else if (message_text.contains("/stop")) {
                executorService.submit(() -> stop(chat_id));
            } else {
                Future<?> future = executorService.submit(() -> searchDetail(message_text, chat_id));
                activeTasks.put(chat_id, future);
            }

        }

    }

    public void start(long chat_id) {
        sendMessage("""
                <b>Привет, для поиска конденсаторов просто напиши сообщение в формате:
                "0805 X7R 0,1мкФ ±5% 50В".\s
                Если что-то не получается напиши /help</b>
                Для остановки поиска, во время выполнения можно написать /stop""", chat_id);
    }

    private void stop(long chat_id) {
        Future<?> future = activeTasks.get(chat_id);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            sendMessage("Текущая операция была остановлена.", chat_id);
        } else {
            sendMessage("Нет активных операций для остановки.", chat_id);
        }
    }

    public void help(long chat_id) {
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
                </blockquote>""", chat_id);
    }

    public void searchDetail(String message_text, long chat_id) {
        List<String> generatedUrls = this.processInputData(message_text);
        long iterations = 0;
        long completeRequests = 0;
        Parser parser = new Parser();
        String completeParsing = "";
        String noResultsText = "No Results Found. Try modifying your search term below or visit our Help Centre.";

        for (String url : generatedUrls) {
            try {
                if (!ConnectionURL.parsePage(url).body().text().contains(noResultsText)) {
                    completeRequests++;
                    iterations++;
                    completeParsing = parser.parsingFoundElement(url);
                    sendMessage("<b>" + iterations + ". Найдено. Ссылка на страницу: \n</b> " + url + completeParsing + "\n", chat_id);
                } else {
                    iterations++;
                    sendMessage("<b>" + iterations + ". <s>Результаты не найдены!</s> Ссылка на страницу: \n</b>" + url + "\n<i>Попробуйте изменить параметры поиска или проверить не допустил ли я ошибку в генерации ссылки.</i>", chat_id);
                }
            } catch (Exception e) {
                sendMessage(e.getMessage(), chat_id);
                throw new RuntimeException(e);
            }

        }

        sendMessage("Обработка завершена.\nЯ смог найти: <b><u>" + completeRequests + " из " + iterations + "</u></b> запросов", chat_id);
    }

    private void sendMessage(String text, long chat_id) {
        SendMessage message = SendMessage
                .builder()
                .parseMode("HTML")
                .chatId(chat_id)
                .text(text)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
