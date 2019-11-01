import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
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
    private final String CurrentUsersPath = DatabasePath + "//Queue";
    private final String GamePath = DatabasePath + "//Game_";
    private final String GameConfig = DatabasePath + "//GamesConfigs";

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
                String gameId = DeleteUser(CurrentUsersPath, "@" + callbackQuery.getFrom().getUserName());
                JSONObject users = readJson(CurrentUsersPath);
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
                JSONObject users = readJson(CurrentUsersPath);
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
        return readJson(CurrentUsersPath).get(username).toString();
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
            message.setChatId(msg.getChatId());
            JSONObject json = new JSONObject();
            if (msg.getFrom().getUserName() != null) {
                try {
                    json = readJson(UsersPath);
                    json.put("@" + msg.getFrom().getUserName(), msg.getChatId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                message.setText("Вы успешно зарегестрировались в игре");

                File file = new File(UsersPath);
                try {
                    FileWriter writer = new FileWriter(file);
                    writer.write(json.toString());
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                message.setText("Добавьте ник чтобы зарегестрироваться");
            }
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        if (msg.getText().equals("/game")) { //TODO вывод колоды
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
            String GameID = "";
            JSONObject json = new JSONObject();
            try {
                json = readJson(CurrentUsersPath);
            } catch (Exception e) {
                    e.printStackTrace();
            }
            if (json.containsKey("@" + msg.getFrom().getUserName())) {
                try {
                    GameID = getGameIdByUsername("@" + msg.getFrom().getUserName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                GameID = UUID.randomUUID().toString();
                json.put("@" + msg.getFrom().getUserName(), GameID);
            }
            String tempUsername = "";
            for (int i = 0; i < msg.getText().length() + 1; ++i) {
                if (msg.getText().length() == i) {
                    if(!isUserOnQueue(tempUsername)) {
                        json.put(tempUsername, GameID);
                    }
                    break;
                }
                if(msg.getText().substring(i, i + 1).equals(" ")) {
                    if(!isUserOnQueue(tempUsername)) {
                        json.put(tempUsername, GameID);
                    }
                    tempUsername = "";
                }
                else tempUsername += msg.getText().substring(i, i + 1);
            }

            File file = new File(CurrentUsersPath);
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
        if (msg.getText().equals("/startGame")) {
            try {
                StartGame("@" + msg.getFrom().getUserName(), getGameIdByUsername("@" + msg.getFrom().getUserName()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (msg.getText().substring(0, 8).equals("/setPack")) {
            try {
                JSONObject configs = readJson(GameConfig);
                configs.put("@" + msg.getFrom().getUserName(), msg.getText().substring(8));

                File file = new File(GameConfig);
                FileWriter writer = new FileWriter(file);
                writer.write(configs.toString());
                writer.flush();
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void StartGame(String username, String gameId) {
        String pack = null;
        JSONObject configs = null;
        try {
            configs = readJson(GameConfig);
            if(configs.containsKey(username)) pack = configs.get(username).toString();
            else {
                pack = "36";
                configs.put(username, "36");
                File file = new File(GameConfig);
                FileWriter writer = new FileWriter(file);
                writer.write(configs.toString());
                writer.flush();
                writer.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //раздать карты и вписать в файл
        List<String> users = GetUsersById(gameId);
        JSONObject usersWithCrads = GiveCards(users, pack);
        if (usersWithCrads == null) {
            SendMessage message = new SendMessage();
            message.setText("Слишком много пользователей");
            try {
                message.setChatId(getUserChatId(username));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        //TODO вынести запись в файл в отдельную функцию, записать в файл пользователе, нарисовать каждому клавиатуру

    }

    private JSONObject GiveCards(List<String> users, String pack) {
        JSONObject usersWithCards = new JSONObject();
        if(users.contains("users more than 8")) {
            return null;
        }
        if(pack.equals("36") && users.size() > 6) pack = "52";
        usersWithCards = GiveCardsToUsers(users, pack);

        return usersWithCards;
    }

    private JSONObject GiveCardsToUsers(List<String> users, String pack) {
        JSONObject returnValue = new JSONObject();
        List<Card> usedCards = new ArrayList<Card>();
        if ((users.size() == 6 && pack.equals("36")) || (users.size() == 9 && pack.equals("52"))) {
            returnValue.put("TrumpCard", Card.getRandomLear());
        } else {
            Card trumpCard = Card.getRandomCard(Short.parseShort(pack));
            usedCards.add(trumpCard);
            returnValue.put("TrumpCard", trumpCard.getStringCard());
        }
        Iterator<String> iter = users.iterator();
        while (iter.hasNext()) {
            JSONObject cards = new JSONObject();
            for (int i = 0; cards.size() < 6; ++i) {
                Card temp = Card.getRandomCard(Short.parseShort(pack));
                while (usedCards.contains(temp)) temp = Card.getRandomCard(Short.parseShort(pack));
                usedCards.add(temp);
                cards.put((i + 1), temp.getStringCard());
            }
            returnValue.put(iter.next(), cards);
        }
        return returnValue;
    }

    private List<String> GetUsersById(String gameId) {
        List<String> res = new ArrayList<String>();
        Object[] users = null;
        try {
            users = readJson(CurrentUsersPath).entrySet().toArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 0; i < users.length && i <= 8; ++i) {
            if(i == 8) {
                res.clear();
                res.add("users more than 8");
                break;
            }
            res.add(users[i].toString().substring(0, users[i].toString().indexOf("=")));
        }
        return res;
    }

    private boolean isUserOnQueue(String username) {
        try {
            return readJson(CurrentUsersPath).containsKey(username);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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
