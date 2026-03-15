package model;

public class PaymentCard {
    private long id;
    private long userId;
    private String cardHolder;
    private String lastFour;
    private String cardType;
    private String expiryMonth;
    private String expiryYear;

    public PaymentCard() {
    }

    public String getDisplayName() {
        String type = cardType != null ? cardType : "Карта";
        return type + " •••• " + lastFour + "   " + expiryMonth + "/" + expiryYear;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long v) {
        this.userId = v;
    }

    public String getCardHolder() {
        return cardHolder;
    }

    public void setCardHolder(String v) {
        this.cardHolder = v;
    }

    public String getLastFour() {
        return lastFour;
    }

    public void setLastFour(String v) {
        this.lastFour = v;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String v) {
        this.cardType = v;
    }

    public String getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(String v) {
        this.expiryMonth = v;
    }

    public String getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(String v) {
        this.expiryYear = v;
    }
}
