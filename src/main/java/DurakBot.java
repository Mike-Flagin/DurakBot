import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
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

                try {
                    WriteToFile(UsersPath, json.toString());
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
        if (msg.getText().equals("/game")) { //TODO вывод размера колоды
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

            try {
                WriteToFile(CurrentUsersPath, json.toString());
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

                WriteToFile(GameConfig, configs.toString());
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
                WriteToFile(GameConfig, configs.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //раздать карты и вписать в файл
        List<String> users = GetUsersById(gameId);
        JSONObject usersWithCards = GiveCards(users, pack);
        if (usersWithCards == null) {
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

        try {
            WriteToFile(GamePath + gameId, usersWithCards.toString());
            DrawKeybords(usersWithCards);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void DrawKeybords(JSONObject usersWithCards) {
        Object[] users = usersWithCards.entrySet().toArray();
        JSONObject cardsLeft = (JSONObject) usersWithCards.get("CardsLeft");
        boolean isTrumpCard = false;
        Card trumpCard = null;
        for (int i = 0; i < users.length; ++i) {
            if(users[i].toString().charAt(0) == '@' && isTrumpCard) {
                String user = users[i].toString().substring(0, users[i].toString().indexOf("="));
                JSONParser jsonParser = new JSONParser();
                JSONObject cards = new JSONObject();
                try {
                    cards = (JSONObject) jsonParser.parse(users[i].toString().substring(users[i].toString().indexOf("=") + 1));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
                KeyboardRow row = new KeyboardRow();
                for (int j = 0; j < cards.size(); ++j) {
                    row.add(new KeyboardButton(cards.get(Integer.toString(j + 1)).toString()));
                }
                keyboard.setKeyboard(Collections.singletonList(row));
                keyboard.setOneTimeKeyboard(false);
                keyboard.setResizeKeyboard(true);
                SendMessage msg = new SendMessage();
                msg.setReplyMarkup(keyboard);
                msg.setText("Участники:\r\n");
                for (int j = 0; j < users.length; ++j) {
                    if (users[j].toString().charAt(0) != '@') continue;
                    msg.setText(msg.getText() + "\r\n" + users[j].toString().substring(0, users[j].toString().indexOf("=")) + ": ");
                    if (users[j].toString().substring(0, users[j].toString().indexOf("=")).equals(user)) {
                        for (int n = 0; n < cards.size(); ++n) {
                            msg.setText(msg.getText() + " " + cards.get(Integer.toString(n + 1)).toString());
                        }
                    } else {
                        try {
                            for (int n = 0; n < ((JSONObject) jsonParser.parse(users[j].toString().substring(users[j].toString().indexOf("=") + 1))).size(); ++n) {
                                msg.setText(msg.getText() + " \uD83C\uDCCF");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                msg.setText(msg.getText() + "\r\n");
                msg.setText(msg.getText() + "\r\nКолода: \uD83C\uDCCFx" + cardsLeft.size());
                msg.setText(msg.getText() + " | Козырь:");
                msg.setText(msg.getText() + trumpCard.getStringCard());
                try {
                    msg.setChatId(getUserChatId(user));
                    execute(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (!isTrumpCard && users[i].toString().substring(0, 9).equals("TrumpCard")) {
                isTrumpCard = true;
                if(users[i].toString().length() == 13) {
                    trumpCard = new Card(users[i].toString().substring(users[i].toString().indexOf("=") + 1, users[i].toString().indexOf("=") + 2),
                            users[i].toString().substring(users[i].toString().indexOf("=") + 2));
                } else trumpCard = new Card(users[i].toString().substring(users[i].toString().indexOf("=") + 1, users[i].toString().indexOf("=") + 2));
                i = -1;
            }
        }
        //функция оповещения игрока о его ходе
    }
    //TODO выбрать кто ходит первым, вести игру

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
                for (int j = 0; j < usedCards.size(); ++j) {
                    if(usedCards.get(j).equals(temp)) {
                        j = 0;
                        temp = Card.getRandomCard(Short.parseShort(pack));
                    }
                }
                usedCards.add(temp);
                cards.put((i + 1), temp.getStringCard());
            }
            returnValue.put(iter.next(), cards);
        }
        returnValue.put("CardsLeft", getLeftCards(usedCards, pack));
        returnValue.put("CurrentTurn", getRandomUser(users));
        return returnValue;
    }

    private String getRandomUser(List<String> users) {
        int randomNumber = (int) (Math.random() * users.size());
        return users.get(randomNumber);
    }

    private JSONObject getLeftCards(List<Card> usedCards, String pack) {
        JSONObject returnValue = new JSONObject();
        int counter = 0;
        if (pack.equals("36")) {
            for (int i = 0; i < 4; ++i) {
                for (int j = 6; j < 15; ++j) {
                    String lear = null;
                    switch (i) {
                        case 0:
                            lear = "♦️";
                            break;
                        case 1:
                            lear = "♥️";
                            break;
                        case 2:
                            lear = "♣️";
                            break;
                        case 3:
                            lear = "♠️";
                            break;
                    }
                    String value = null;
                    switch (j) {
                        case 11:
                            value = "J";
                            break;
                        case 12:
                            value = "Q";
                            break;
                        case 13:
                            value = "K";
                            break;
                        case 14:
                            value = "A";
                            break;
                        default:
                            value = Integer.toString(j);
                    }
                    Card temp = new Card(lear, value);
                    boolean isUnique = true;
                    for (int n = 0; n < usedCards.size(); ++n) {
                        if(usedCards.get(n).equals(temp)) {
                            isUnique = false;
                            break;
                        }
                    }
                    if (isUnique) returnValue.put(Integer.toString(++counter), temp.getStringCard());
                }
            }
        } else {
            for (int i = 0; i < 4; ++i) {
                for (int j = 2; j < 15; ++j) {
                    String lear = null;
                    switch (i) {
                        case 0:
                            lear = "♦️";
                            break;
                        case 1:
                            lear = "♥️";
                            break;
                        case 2:
                            lear = "♣️";
                            break;
                        case 3:
                            lear = "♠️";
                            break;
                    }
                    String value = null;
                    switch (j) {
                        case 11:
                            value = "J";
                            break;
                        case 12:
                            value = "Q";
                            break;
                        case 13:
                            value = "K";
                            break;
                        case 14:
                            value = "A";
                            break;
                        default:
                            value = Integer.toString(j);
                    }
                    Card temp = new Card(lear, value);
                    boolean isUnique = true;
                    for (int n = 0; n < usedCards.size(); ++n) {
                        if(usedCards.get(n).equals(temp)) {
                            isUnique = false;
                            break;
                        }
                    }
                    if (isUnique) returnValue.put(Integer.toString(++counter), temp.getStringCard());
                }
            }
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

    private void WriteToFile(String filename, String data) throws IOException {
        File file = new File(filename);
        FileWriter writer = new FileWriter(file);
        writer.write(data);
        writer.flush();
        writer.close();
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
