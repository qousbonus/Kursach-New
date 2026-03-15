package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.PaymentCard;
import model.User;
import repository.PaymentCardRepository;
import service.AuthService;
import service.UserService;
import util.AlertHelper;
import util.SceneManager;
import util.Session;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML
    private Label usernameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private Label profileErrorLabel;

    @FXML
    private TextField newEmailField;
    @FXML
    private Label emailErrorLabel;
    @FXML
    private Label emailSuccessLabel;

    @FXML
    private VBox cardsBox;
    @FXML
    private Label noCardsLabel;
    @FXML
    private TextField cardNumberField;
    @FXML
    private TextField cardHolderField;
    @FXML
    private TextField expiryMonthField;
    @FXML
    private TextField expiryYearField;
    @FXML
    private PasswordField cvvField;
    @FXML
    private Label cardErrorLabel;
    @FXML
    private VBox cardsSection;

    private final UserService userSvc = new UserService();
    private final PaymentCardRepository cardRepo = new PaymentCardRepository();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User u = Session.getInstance().getCurrentUser();
        usernameLabel.setText(u.getUsername());
        emailLabel.setText(u.getEmail());
        firstNameField.setText(u.getFirstName() != null ? u.getFirstName() : "");
        lastNameField.setText(u.getLastName() != null ? u.getLastName() : "");
        phoneField.setText(u.getPhone() != null ? u.getPhone() : "");

        if (roleLabel != null) {
            boolean mgr = "manager".equals(u.getRole());
            roleLabel.setText(mgr ? "Менеджер" : "Покупатель");
            roleLabel.setStyle(mgr
                    ? "-fx-text-fill:#C6FF00; -fx-font-size:11px; -fx-font-weight:bold;"
                    : "-fx-text-fill:#AAAAAA; -fx-font-size:11px;");
        }

        boolean mgr = "manager".equals(u.getRole());
        if (cardsSection != null) {
            cardsSection.setVisible(!mgr);
            cardsSection.setManaged(!mgr);
        }

        if (!mgr) {
            addCardNumberFormatter();
            addExpiryFormatters();
            loadCards();
        }
    }

    private void addCardNumberFormatter() {
        cardNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String digits = newVal.replaceAll("[^\\d]", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) sb.append("  ");
                sb.append(digits.charAt(i));
            }
            String result = sb.toString();
            if (!result.equals(newVal)) {
                cardNumberField.setText(result);
                cardNumberField.positionCaret(result.length());
            }
        });
    }

    private void addExpiryFormatters() {
        expiryMonthField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String d = newVal.replaceAll("[^\\d]", "");
            if (d.length() > 2) d = d.substring(0, 2);
            if (!d.equals(newVal)) expiryMonthField.setText(d);
            if (d.length() == 2) expiryYearField.requestFocus();
        });
        expiryYearField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String d = newVal.replaceAll("[^\\d]", "");
            if (d.length() > 2) d = d.substring(0, 2);
            if (!d.equals(newVal)) expiryYearField.setText(d);
        });
    }

    private void loadCards() {
        cardsBox.getChildren().clear();
        try {
            long uid = Session.getInstance().getCurrentUser().getId();
            List<PaymentCard> cards = cardRepo.findByUser(uid);
            noCardsLabel.setVisible(cards.isEmpty());
            noCardsLabel.setManaged(cards.isEmpty());
            cards.forEach(c -> cardsBox.getChildren().add(buildCardRow(c)));
        } catch (Exception e) {
            noCardsLabel.setVisible(true);
            noCardsLabel.setManaged(true);
        }
    }

    private HBox buildCardRow(PaymentCard card) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:#FFFFFF; -fx-border-color:#E8E8F0; -fx-border-width:1.5;" +
                "-fx-border-radius:12; -fx-background-radius:12; -fx-padding:14 18;");

        Label icon = new Label(cardIcon(card.getCardType()));
        icon.setStyle("-fx-font-size:24px;");

        VBox info = new VBox(3);
        Label name = new Label(card.getDisplayName());
        name.setStyle("-fx-font-weight:bold; -fx-font-size:14px; -fx-text-fill:#111111;");
        Label holder = new Label(card.getCardHolder() != null ? card.getCardHolder() : "");
        holder.setStyle("-fx-text-fill:#AAAAAA; -fx-font-size:11px;");
        info.getChildren().addAll(name, holder);
        HBox.setHgrow(info, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button del = new Button("🗑");
        String delBase = "-fx-background-color:transparent; -fx-border-color:transparent; -fx-text-fill:#DDDDDD; -fx-cursor:hand; -fx-font-size:16px; -fx-padding:4 8;";
        del.setStyle(delBase);
        del.setOnMouseEntered(e -> del.setStyle("-fx-background-color:#FFF0F1; -fx-border-color:transparent; -fx-text-fill:#E8001D; -fx-cursor:hand; -fx-font-size:16px; -fx-padding:4 8; -fx-background-radius:8;"));
        del.setOnMouseExited(e -> del.setStyle(delBase));
        del.setOnAction(e -> {
            if (AlertHelper.confirm("Удаление карты", "Удалить " + card.getDisplayName() + "?")) {
                try {
                    cardRepo.delete(card.getId());
                    loadCards();
                } catch (Exception ex) {
                    AlertHelper.error("Ошибка", ex.getMessage());
                }
            }
        });

        row.getChildren().addAll(icon, info, del);
        return row;
    }

    private String cardIcon(String type) {
        if (type == null) return "💳";
        return switch (type.toLowerCase()) {
            case "visa" -> "💙";
            case "mastercard" -> "🟠";
            case "мир" -> "🇷🇺";
            default -> "💳";
        };
    }

    @FXML
    public void handleSave() {
        profileErrorLabel.setText("");
        User u = Session.getInstance().getCurrentUser();
        u.setFirstName(firstNameField.getText().trim());
        u.setLastName(lastNameField.getText().trim());
        u.setPhone(phoneField.getText().trim());
        String err = userSvc.updateProfile(u);
        if (err != null) profileErrorLabel.setText(err);
        else AlertHelper.info("Профиль", "Данные успешно сохранены!");
    }

    @FXML
    public void handleUpdateEmail() {
        emailErrorLabel.setText("");
        emailSuccessLabel.setText("");
        String newEmail = newEmailField.getText().trim();
        if (newEmail.isEmpty() || !newEmail.contains("@")) {
            emailErrorLabel.setText("Введите корректный email");
            return;
        }
        User u = Session.getInstance().getCurrentUser();
        u.setEmail(newEmail.toLowerCase());
        String err = userSvc.updateEmail(u);
        if (err != null) {
            emailErrorLabel.setText(err);
        } else {
            emailSuccessLabel.setText("Email успешно обновлён");
            emailLabel.setText(newEmail.toLowerCase());
            newEmailField.clear();
        }
    }

    @FXML
    public void handleAddCard() {
        cardErrorLabel.setText("");
        String raw = cardNumberField.getText().replaceAll("[^\\d]", "");
        String holder = cardHolderField.getText().trim().toUpperCase();
        String cvv = cvvField.getText().trim();

        if (raw.length() != 16) {
            cardErrorLabel.setText("Введите полный 16-значный номер карты");
            return;
        }
        if (!expiryMonthField.getText().trim().matches("\\d{2}")) {
            cardErrorLabel.setText("Введите месяц (2 цифры, например 09)");
            return;
        }
        if (!expiryYearField.getText().trim().matches("\\d{2}")) {
            cardErrorLabel.setText("Введите год (2 цифры, например 27)");
            return;
        }
        if (holder.isEmpty()) {
            cardErrorLabel.setText("Введите имя держателя карты");
            return;
        }
        if (cvv.length() < 3) {
            cardErrorLabel.setText("Введите CVV (3 цифры на обороте карты)");
            return;
        }

        String cardType = detectCardType(raw);
        PaymentCard card = new PaymentCard();
        card.setUserId(Session.getInstance().getCurrentUser().getId());
        card.setLastFour(raw.substring(12));
        card.setCardType(cardType);
        card.setExpiryMonth(expiryMonthField.getText().trim());
        card.setExpiryYear(expiryYearField.getText().trim());
        card.setCardHolder(holder);

        try {
            cardRepo.save(card);
            cardNumberField.clear();
            expiryMonthField.clear();
            expiryYearField.clear();
            cardHolderField.clear();
            cvvField.clear();
            loadCards();
            AlertHelper.info("Карта привязана", cardIcon(cardType) + "  " + cardType + " •••• " + card.getLastFour() + " успешно добавлена");
        } catch (Exception e) {
            cardErrorLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    private String detectCardType(String number) {
        if (number.startsWith("4")) return "Visa";
        if (number.startsWith("2200") || number.startsWith("2201")
                || number.startsWith("2202") || number.startsWith("2203")
                || number.startsWith("2204")) return "Мир";
        if (number.startsWith("5") || number.startsWith("2")) return "Mastercard";
        return "Другое";
    }

    @FXML
    public void handleBack() {
        SceneManager.showCatalog();
    }

    @FXML
    public void handleOrders() {
        SceneManager.showOrders();
    }

    @FXML
    public void handleOrdersClick() {
        SceneManager.showOrders();
    }

    @FXML
    public void handleLogout() {
        new AuthService().logout();
        SceneManager.showLogin();
    }
}
