import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.util.*;

public class DurakBot extends TelegramLongPollingBot {
    private final String DatabasePath = "D://misha//Telegram//DurakBot//DurakBotDatabase";
    private final String UsersPath = DatabasePath + "//Users";
    private final String UsersQueue = DatabasePath + "//Queue";

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasCallbackQuery()) {
            AnsCallback(update.getCallbackQuery());
        } else if (update.hasMessage()) {
            Message msg = update.getMessage();
            ReplyToMessage(msg);
        }
    }

    private void AnsCallback(CallbackQuery callbackQuery) {
        if(callbackQuery.getData().equals("Deny")) {
            String filename = "";
            for(int i = callbackQuery.getMessage().getText().indexOf("@") + 1; callbackQuery.getMessage().getText().charAt(i) != ' '; ++i) {
                filename += callbackQuery.getMessage().getText().charAt(i);
            }
            try {
                String gameId = DeleteUser(UsersQueue, "@" + callbackQuery.getFrom().getUserName());
                JSONObject users = readJson(UsersQueue);
                Object[] a = users.entrySet().toArray();
                SendMessage msg = new SendMessage();
                msg.setChatId(getUserChatId("@" + callbackQuery.getFrom().getUserName()));
                msg.setText("Вы отказались от игры");
                execute(msg);
                for(int i = 0; i < a.length && users.containsValue(gameId); ++i) {
                    if(a[i].toString().substring(a[i].toString().indexOf('=') + 1).equals(gameId)) {
                        msg.setChatId(getUserChatId(a[i].toString().substring(0, a[i].toString().indexOf('='))));
                        msg.setText("Пользователь @" + callbackQuery.getFrom().getUserName() + " отказался от игры");
                        execute(msg);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(callbackQuery.getData().equals("Accept")) {
            try {
                String gameId = getGameIdByUsername("@" + callbackQuery.getFrom().getUserName());
                JSONObject users = readJson(UsersQueue);
                Object[] a = users.entrySet().toArray();
                SendMessage msg = new SendMessage();
                for(int i = 0; i < a.length && users.containsValue(gameId); ++i) {
                    if(a[i].toString().substring(a[i].toString().indexOf('=') + 1).equals(gameId)) {
                        msg.setChatId(getUserChatId(a[i].toString().substring(0, a[i].toString().indexOf('='))));
                        if((a[i].toString().substring(0, a[i].toString().indexOf('='))).equals("@" + callbackQuery.getFrom().getUserName())) {
                            msg.setText("Вы согласились на игру");
                        } else {
                            msg.setText("Пользователь @" + callbackQuery.getFrom().getUserName() + " согласился на игру");
                        }
                        execute(msg);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getGameIdByUsername(String username) throws Exception {
        return readJson(UsersQueue).get(username).toString();
    }

    private String getUserChatId(String username) throws Exception {
        return readJson(UsersPath).get(username).toString();
    }

    private String DeleteUser(String filename, String user) throws Exception {
        JSONObject users = readJson(filename);
        String id = (String) users.get(user);
        users.remove(user);
        FileWriter writer = new FileWriter(filename);
        writer.write(users.toString());
        writer.flush();
        writer.close();
        return id;
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
                json = readJson(UsersPath);
                json.put("@" + msg.getFrom().getUserName(), msg.getChatId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            File file = new File(UsersPath);
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
            String GameID = UUID.randomUUID().toString();
            JSONObject json = new JSONObject();
            String tempUsername = "";
            for (int i = 0; i < msg.getText().length() + 1; ++i) {
                if (msg.getText().length() == i) {
                    json.put(tempUsername, GameID);
                    break;
                }
                if(msg.getText().substring(i, i + 1).equals(" ")) {
                    json.put(tempUsername, GameID);
                    tempUsername = "";
                }
                else tempUsername += msg.getText().substring(i, i + 1);
            }
            json.put("@" + msg.getFrom().getUserName(), GameID);

            File file = new File(UsersQueue);
            try {
                FileWriter writer = new FileWriter(file);
                writer.write(json.toString());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                SendInvitation(json, "@" + msg.getFrom().getUserName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void SendInvitation(JSONObject users, String FromUserName) throws Exception {
        Object[] usersArray = users.keySet().toArray();
        for(int i = 0; i < usersArray.length; ++i) {
            if(usersArray[i].equals(FromUserName)) continue;
            SendMessage message = new SendMessage();
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            buttons.add(new InlineKeyboardButton().setText("Да").setCallbackData("Accept"));
            buttons.add(new InlineKeyboardButton().setText("Нет").setCallbackData("Deny"));
            keyboard.setKeyboard(Collections.singletonList(buttons));
            message.setText("Пользователь " + FromUserName + " приглашает вас играть в дурака");
            message.setChatId(getUserChatId((String) usersArray[i]));
            message.setReplyMarkup(keyboard);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "CardsDurakBot";
    }

    @Override
    public String getBotToken() {
            return "833939584:AAGB0ddidM-zbMWTeqnRUvr-puQ9W0P1Zrc";
    }

    private static JSONObject readJson(String filename) throws Exception {
        FileReader reader = new FileReader(filename);
        JSONParser jsonParser = new JSONParser();
        return (JSONObject) jsonParser.parse(reader);
    }
}
