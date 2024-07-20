import io.github.szrnkapeter.firebase.hosting.FirebaseHostingApiClient;
import io.github.szrnkapeter.firebase.hosting.builder.FirebaseHostingApiConfigBuilder;
import io.github.szrnkapeter.firebase.hosting.config.FirebaseHostingApiConfig;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
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
