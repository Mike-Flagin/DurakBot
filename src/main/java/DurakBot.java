import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DurakBot extends TelegramLongPollingBot {
    private String GamesPath = "D://misha//Telegram//DurakBot//DurakBotDatabase";

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasCallbackQuery()) {
            //TODO
        } else if (update.hasMessage()) {
            Message msg = update.getMessage();
            ReplyToMessage(msg);
        }
    }

    private void ReplyToMessage(Message msg) {
        if (msg.getText().equals("/start")) {
            //TODO: Greeting
            SendMessage message = new SendMessage();
            message.setText("Hello world");
            message.setChatId(msg.getChatId());
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            JSONObject json = new JSONObject();
            try {
                json = (JSONObject) readJson(GamesPath + "//Users");
                json.put("@" + msg.getFrom().getUserName(), msg.getChatId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            File file = new File(GamesPath + "//Users");
            try {
                FileWriter writer = new FileWriter(file);
                writer.write(json.toString());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (msg.getText().equals("/game")) {
            SendMessage message = new SendMessage();
            message.setText("Введите никнеймы игроков, которых вы хотите пригласить, через пробел, например: @username1 @username2");
            message.setChatId(msg.getChatId());
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        if (msg.getText().substring(0, 1).equals("@")) {
            JSONObject json = new JSONObject();
            String tempUsername = "";
            for (int i = 0, userCounter = 0; i < msg.getText().length() + 1; ++i) {
                if (msg.getText().length() == i) {
                    json.put("Username" + (++userCounter), tempUsername);
                    break;
                }
                if(msg.getText().substring(i, i + 1).equals(" ")) {
                    json.put("Username" + (++userCounter), tempUsername);
                    tempUsername = "";
                }
                else tempUsername += msg.getText().substring(i, i + 1);
            }

            File file = new File(GamesPath + "//Game_" + getDate());
            try {
                if(file.createNewFile()) {
                    FileWriter writer = new FileWriter(file);
                    writer.write(json.toString());
                    writer.flush();
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                SendInvitation(json, msg.getFrom().getUserName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void SendInvitation(JSONObject json, String userName) throws Exception {
        for (int i = 1; i <= json.size(); ++i){
            SendMessage message = new SendMessage();
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            buttons.add(new InlineKeyboardButton().setText("Да").setCallbackData("Accept"));
            buttons.add(new InlineKeyboardButton().setText("Нет").setCallbackData("Deny"));
            keyboard.setKeyboard(Collections.singletonList(buttons));
            message.setText("Пользователь " + userName + " приглашает вас играть в дурака");
            message.setChatId(((JSONObject) readJson(GamesPath + "//Users")).get(json.get("Username" + i)).toString());
            message.setReplyMarkup(keyboard);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private String getDate() {
        Date date = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
        return formatForDateNow.format(date);
    }

    @Override
    public String getBotUsername() {
        return "CardsDurakBot";
    }

    @Override
    public String getBotToken() {
            return "833939584:AAGB0ddidM-zbMWTeqnRUvr-puQ9W0P1Zrc";
    }

    private static Object readJson(String filename) throws Exception {
        FileReader reader = new FileReader(filename);
        JSONParser jsonParser = new JSONParser();
        return jsonParser.parse(reader);
    }
}
