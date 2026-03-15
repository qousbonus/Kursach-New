package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import model.CartItem;
import model.PaymentCard;
import model.PromoCode;
import repository.PaymentCardRepository;
import service.OrderService;
import util.AlertHelper;
import util.SceneManager;
import util.Session;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CartController implements Initializable {

    private static final String NEW_CARD_OPTION = "Новая карта онлайн";

    @FXML
    private VBox cartItemsBox;
    @FXML
    private Label totalLabel;
    @FXML
    private Label discountLabel;
    @FXML
    private Label finalTotalLabel;
    @FXML
    private TextField promoField;
    @FXML
    private Label promoStatusLabel;
    @FXML
    private TextField addressField;
    @FXML
    private ComboBox<String> paymentCombo;
    @FXML
    private TextArea noteArea;
    @FXML
    private Label errorLabel;
    @FXML
    private Button checkoutBtn;
    @FXML
    private Label emptyLabel;

    private final OrderService orders = new OrderService();
    private final PaymentCardRepository cardRepo = new PaymentCardRepository();
    private PromoCode appliedPromo;
    private List<PaymentCard> savedCards = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadPaymentOptions();
        renderCart();
    }

    private void loadPaymentOptions() {
        paymentCombo.getItems().clear();
        paymentCombo.setOnAction(null);

        if (Session.getInstance().isLoggedIn()) {
            try {
                savedCards = cardRepo.findByUser(Session.getInstance().getCurrentUser().getId());
                savedCards.forEach(c -> paymentCombo.getItems().add(c.getDisplayName()));
            } catch (Exception ignored) {
            }
        }

        paymentCombo.getItems().add(NEW_CARD_OPTION);
        if (!paymentCombo.getItems().isEmpty()) paymentCombo.getSelectionModel().selectFirst();
    }

    private void renderCart() {
        cartItemsBox.getChildren().clear();
        List<CartItem> items = Session.getInstance().getCart();
        setEmptyState(items.isEmpty());

        for (CartItem item : items) {
            try {
                FXMLLoader loader = util.FxmlUtil.loader("/fxml/cart_item.fxml");
                Parent row = loader.load();
                CartItemController ctrl = loader.getController();
                ctrl.setCartItem(item, this::refreshTotals, this::renderCart);
                cartItemsBox.getChildren().add(row);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        refreshTotals();
    }

    private void setEmptyState(boolean empty) {
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);
        checkoutBtn.setDisable(empty);
    }

    private void refreshTotals() {
        List<CartItem> items = Session.getInstance().getCart();
        boolean empty = items.isEmpty();
        setEmptyState(empty);
        if (empty) cartItemsBox.getChildren().clear();

        double total = Session.getInstance().getCartTotal();
        double discount = appliedPromo != null ? total * appliedPromo.getDiscountPercent() / 100.0 : 0;
        totalLabel.setText(String.format("%.0f руб.", total));
        discountLabel.setText(discount > 0 ? String.format("-%.0f руб.", discount) : "—");
        finalTotalLabel.setText(String.format("%.0f руб.", total - discount));
    }

    @FXML
    public void handleApplyPromo() {
        String code = promoField.getText().trim();
        if (code.isEmpty()) {
            promoStatusLabel.setText("Введите промокод");
            return;
        }

        PromoCode pc = orders.validatePromo(code);
        if (pc != null) {
            appliedPromo = pc;
            promoStatusLabel.setText("Скидка " + pc.getDiscountPercent() + "% применена!");
            promoStatusLabel.getStyleClass().remove("text-error");
            if (!promoStatusLabel.getStyleClass().contains("text-success"))
                promoStatusLabel.getStyleClass().add("text-success");
        } else {
            appliedPromo = null;
            promoStatusLabel.setText("Промокод недействителен или истёк");
            promoStatusLabel.getStyleClass().remove("text-success");
            if (!promoStatusLabel.getStyleClass().contains("text-error"))
                promoStatusLabel.getStyleClass().add("text-error");
        }
        refreshTotals();
    }

    @FXML
    public void handleCheckout() {
        String payment = paymentCombo.getValue();
        if (payment == null || payment.isBlank()) {
            errorLabel.setText("Выберите способ оплаты");
            return;
        }
        if (NEW_CARD_OPTION.equals(payment)) {
            errorLabel.setText("Добавьте карту в профиле и выберите её для оплаты");
            if (AlertHelper.confirm("Добавить карту?",
                    "Для оплаты картой необходимо сначала добавить её в профиле.\nПерейти в профиль?"))
                SceneManager.showProfile();
            return;
        }
        String promoCode = appliedPromo != null ? appliedPromo.getCode() : promoField.getText();
        String err = orders.placeOrder(addressField.getText(), payment, promoCode, noteArea.getText());
        if (err != null) {
            errorLabel.setText(err);
        } else {
            AlertHelper.info("Заказ оформлен!", "Ваш заказ успешно принят");
            SceneManager.showOrders();
        }
    }

    @FXML
    public void handleBack() {
        SceneManager.showCatalog();
    }

    @FXML
    public void handleOrders() {
        SceneManager.showOrders();
    }
}
