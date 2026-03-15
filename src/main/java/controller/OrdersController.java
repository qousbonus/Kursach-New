package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.Order;
import service.OrderService;
import util.SceneManager;
import util.Session;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class OrdersController implements Initializable {

    @FXML
    private VBox ordersBox;
    @FXML
    private Label emptyLabel;

    private final OrderService orderSvc = new OrderService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadOrders();
    }

    private void loadOrders() {
        ordersBox.getChildren().clear();
        long uid = Session.getInstance().getCurrentUser().getId();
        List<Order> orders = orderSvc.getUserOrders(uid);

        emptyLabel.setVisible(orders.isEmpty());
        emptyLabel.setManaged(orders.isEmpty());

        for (Order o : orders) {
            try {
                FXMLLoader loader = util.FxmlUtil.loader("/fxml/order_card.fxml");
                Parent card = loader.load();
                OrderCardController ctrl = loader.getController();
                ctrl.setOrder(o, this::loadOrders);
                ordersBox.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void handleBack() {
        SceneManager.showCatalog();
    }

    @FXML
    public void handleProfile() {
        SceneManager.showProfile();
    }
}
