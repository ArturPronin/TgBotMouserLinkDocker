import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

public class Main {

    public static void main(String[] args) {
        MouserLinkBot mouserLinkBot = new MouserLinkBot();
        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(mouserLinkBot.getBotToken(), mouserLinkBot);
            System.out.println("Mouser Link Bot Strated!");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
