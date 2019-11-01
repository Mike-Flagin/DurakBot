public class Card {
    public String value;
    public String lear;

    public Card(String lear, String value) {
        this.value = value;
        this.lear = lear;
    }

    public Card(String lear) {
        this.lear = lear;
    }

    public static String getRandomLear() {
        switch ((int) (Math.random() * 4)) {
            case 0: return "♦️";
            case 1: return "♥️";
            case 2: return "♣️";
            case 3: return "♠️";
            default: return null;
        }
    }

    public static String getRandomValue(short range) {
        if (range == 36) {
            switch ((int) (Math.random() * 9)) {
                case 0: return "6";
                case 1: return "7";
                case 2: return "8";
                case 3: return "9";
                case 4: return "10";
                case 5: return "J";
                case 6: return "Q";
                case 7: return "K";
                case 8: return "A";
            }
        }
        if (range == 52) {
            switch ((int) (Math.random() * 13)) {
                case 0: return "2";
                case 1: return "3";
                case 2: return "4";
                case 3: return "5";
                case 4: return "6";
                case 5: return "7";
                case 6: return "8";
                case 7: return "9";
                case 8: return "10";
                case 9: return "J";
                case 10: return "Q";
                case 11: return "K";
                case 12: return "A";
            }
        }
        return null;
    }

    public static Card getRandomCard(short range) {
        return new Card(getRandomLear(), getRandomValue(range));
    }

    String getValue() {
        return value;
    }

    String getLear() {
        return lear;
    }

    String getStringCard() {
        return getLear() + getValue();
    }
}