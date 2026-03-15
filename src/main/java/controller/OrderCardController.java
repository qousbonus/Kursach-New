package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.Order;
import model.OrderItem;
import service.OrderService;
import util.AlertHelper;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class OrderCardController implements Initializable {

    @FXML
    private Label orderIdLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label addressLabel;
    @FXML
    private VBox itemsBox;
    @FXML
    private Button markReceivedBtn;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private Order order;
    private Runnable onStatusChanged;
    private final OrderService orderSvc = new OrderService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    public void setOrder(Order order) {
        setOrder(order, null);
    }

    public void setOrder(Order order, Runnable onStatusChanged) {
        this.order = order;
        this.onStatusChanged = onStatusChanged;
        bind();
    }

    private void bind() {
        orderIdLabel.setText("Заказ #" + order.getId());
        statusLabel.setText(order.getStatusDisplay());

        String css = switch (order.getStatus() == null ? "" : order.getStatus()) {
            case "shipped" -> "status-shipped";
            case "delivered" -> "status-delivered";
            case "cancelled" -> "status-cancelled";
            default -> "status-pending";
        };
        statusLabel.getStyleClass().setAll(css);

        dateLabel.setText(order.getCreatedAt() != null ? order.getCreatedAt().format(FMT) : "");
        totalLabel.setText(String.format("Итого: %.0f руб.", order.getFinalAmount()));
        addressLabel.setText(order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "");

        itemsBox.getChildren().clear();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Label lbl = new Label(item.getProductName() + "  x" + item.getQuantity()
                        + "  —  " + String.format("%.0f руб.", item.getSubtotal()));
                lbl.getStyleClass().add("order-item-label");
                itemsBox.getChildren().add(lbl);
            }
        }

        boolean canMark = "shipped".equals(order.getStatus());
        if (markReceivedBtn != null) {
            markReceivedBtn.setVisible(canMark);
            markReceivedBtn.setManaged(canMark);
        }
    }

    @FXML
    public void handleMarkReceived() {
        if (order == null) return;
        if (!AlertHelper.confirm("Подтверждение", "Отметить заказ #" + order.getId() + " как полученный?")) return;
        String err = orderSvc.updateStatus(order.getId(), "delivered");
        if (err != null) {
            AlertHelper.error("Ошибка", err);
        } else {
            order.setStatus("delivered");
            bind();
            if (onStatusChanged != null) onStatusChanged.run();
        }
    }
}
