import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class DurakBot extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {
        
    }

    @Override
    public String getBotUsername() {
        return "CardsDurakBot";
    }

    @Override
    public String getBotToken() {
            return "833939584:AAGB0ddidM-zbMWTeqnRUvr-puQ9W0P1Zrc";
    }
}
