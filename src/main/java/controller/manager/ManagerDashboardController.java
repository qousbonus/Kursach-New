package controller.manager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Order;
import model.Product;
import model.PromoCode;
import service.OrderService;
import service.ProductService;
import service.PromoService;
import util.AlertHelper;
import util.SceneManager;
import util.Session;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ManagerDashboardController implements Initializable {

    @FXML
    private Label managerNameLabel;
    @FXML
    private Button tabProductsBtn, tabOrdersBtn, tabPromosBtn;
    @FXML
    private VBox tabProducts, tabOrders, tabPromos;

    @FXML
    private TableView<Product> productsTable;
    @FXML
    private TableColumn<Product, String> colProdName;
    @FXML
    private TableColumn<Product, String> colProdBrand;
    @FXML
    private TableColumn<Product, String> colProdCategory;
    @FXML
    private TableColumn<Product, Double> colProdPrice;
    @FXML
    private TableColumn<Product, Integer> colProdDiscount;
    @FXML
    private TableColumn<Product, Integer> colProdStock;
    @FXML
    private TableColumn<Product, Boolean> colProdAvailable;
    @FXML
    private TableColumn<Product, Void> colProdActions;
    @FXML
    private TextField productSearchField;

    @FXML
    private TableView<Order> ordersTable;
    @FXML
    private TableColumn<Order, Long> colOrderId;
    @FXML
    private TableColumn<Order, String> colOrderUser;
    @FXML
    private TableColumn<Order, String> colOrderStatus;
    @FXML
    private TableColumn<Order, String> colOrderDate;
    @FXML
    private TableColumn<Order, Double> colOrderTotal;
    @FXML
    private TableColumn<Order, String> colOrderAddress;
    @FXML
    private ComboBox<String> orderStatusFilter;
    @FXML
    private ComboBox<String> newStatusCombo;

    @FXML
    private TableView<PromoCode> promosTable;
    @FXML
    private TableColumn<PromoCode, String> colPromoCode;
    @FXML
    private TableColumn<PromoCode, Integer> colPromoDiscount;
    @FXML
    private TableColumn<PromoCode, Integer> colPromoUses;
    @FXML
    private TableColumn<PromoCode, Integer> colPromoMax;
    @FXML
    private TableColumn<PromoCode, LocalDateTime> colPromoExpires;
    @FXML
    private TableColumn<PromoCode, Boolean> colPromoActive;
    @FXML
    private TableColumn<PromoCode, Void> colPromoActions;

    private final ProductService prodSvc = new ProductService();
    private final OrderService orderSvc = new OrderService();
    private final PromoService promoSvc = new PromoService();

    private List<Product> allProducts;
    private List<Order> allOrders;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        managerNameLabel.setText(Session.getInstance().getCurrentUser().getFullName());
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        promosTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupProductsTable();
        setupOrdersTable();
        setupPromosTable();
        loadProducts();
        loadOrders();
        productSearchField.textProperty().addListener((obs, o, n) -> filterProducts(n));

        orderStatusFilter.getItems().addAll("Все", "Ожидание", "Доставка", "Получено");
        orderStatusFilter.setValue("Все");
        orderStatusFilter.setOnAction(e -> filterOrders());
        newStatusCombo.getItems().addAll("Ожидание", "Доставка", "Получено");
        newStatusCombo.setValue("Доставка");
    }


    private void setupProductsTable() {
        colProdName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProdBrand.setCellValueFactory(new PropertyValueFactory<>("brand"));
        colProdCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colProdPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colProdPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : String.format("%.0f руб.", val));
            }
        });
        colProdDiscount.setCellValueFactory(new PropertyValueFactory<>("discountPercent"));
        colProdDiscount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : (val > 0 ? "-" + val + "%" : "—"));
            }
        });
        colProdStock.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));
        colProdStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(String.valueOf(val));
                setStyle(val == 0 ? "-fx-text-fill:#F87171;" : val < 10 ? "-fx-text-fill:#FBB040;" : "-fx-text-fill:#4ADE80;");
            }
        });
        colProdAvailable.setCellValueFactory(new PropertyValueFactory<>("available"));
        colProdAvailable.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    return;
                }
                setText(val ? "Да" : "Нет");
                setStyle(val ? "-fx-text-fill:#4ADE80;" : "-fx-text-fill:#F87171;");
            }
        });
        colProdActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Изменить");
            private final Button hideBtn = new Button("Скрыть");
            private final Button delBtn = new Button("Удалить");
            private final HBox box = new HBox(5, editBtn, hideBtn, delBtn);

            {
                String base = "-fx-border-radius:5; -fx-background-radius:5; -fx-cursor:hand; -fx-font-size:11px; -fx-padding:3 7;";
                editBtn.setStyle("-fx-background-color:#1E2A3A; -fx-text-fill:#60A5FA; -fx-border-color:#2D3D50;" + base);
                hideBtn.setStyle("-fx-background-color:#2A2A1A; -fx-text-fill:#FBB040; -fx-border-color:#3A3A2A;" + base);
                delBtn.setStyle("-fx-background-color:#3A1E1E; -fx-text-fill:#F87171; -fx-border-color:#502D2D;" + base);
                editBtn.setOnAction(e -> openProductForm(getTableView().getItems().get(getIndex())));
                hideBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    if (!AlertHelper.confirm("Деактивация", "Скрыть товар «" + p.getName() + "» из каталога?")) return;
                    String err = prodSvc.deactivate(p.getId());
                    if (err != null) AlertHelper.error("Ошибка", err);
                    else loadProducts();
                });
                delBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    if (!AlertHelper.confirm("Удаление", "Полностью удалить товар «" + p.getName() + "»? Это действие необратимо!"))
                        return;
                    String err = prodSvc.hardDelete(p.getId());
                    if (err != null) AlertHelper.error("Ошибка", err);
                    else loadProducts();
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadProducts() {
        allProducts = prodSvc.getAll();
        productsTable.getItems().setAll(allProducts);
    }

    private void filterProducts(String query) {
        if (query == null || query.isBlank()) {
            productsTable.getItems().setAll(allProducts);
            return;
        }
        String q = query.toLowerCase();
        productsTable.getItems().setAll(allProducts.stream()
                .filter(p -> p.getName().toLowerCase().contains(q)
                        || (p.getBrand() != null && p.getBrand().toLowerCase().contains(q)))
                .toList());
    }


    private void setupOrdersTable() {
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOrderUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colOrderStatus.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));
        colOrderStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(val);
                Order row = getTableRow() != null ? (Order) getTableRow().getItem() : null;
                setStyle(row == null ? "" : switch (row.getStatus() == null ? "" : row.getStatus()) {
                    case "delivered" -> "-fx-text-fill:#4ADE80;";
                    case "shipped" -> "-fx-text-fill:#2DD4BF;";
                    default -> "-fx-text-fill:#FBB040;";
                });
            }
        });
        colOrderDate.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colOrderTotal.setCellValueFactory(d ->
                new javafx.beans.property.SimpleDoubleProperty(
                        d.getValue() != null ? d.getValue().getFinalAmount() : 0).asObject());
        colOrderTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : String.format("%.0f руб.", val));
            }
        });
        colOrderAddress.setCellValueFactory(new PropertyValueFactory<>("deliveryAddress"));
    }

    private void loadOrders() {
        allOrders = orderSvc.getAllOrders();
        ordersTable.getItems().setAll(allOrders);
    }

    private void filterOrders() {
        String f = orderStatusFilter.getValue();
        if (f == null || f.equals("Все")) {
            ordersTable.getItems().setAll(allOrders);
            return;
        }
        String status = switch (f) {
            case "Ожидание" -> "pending";
            case "Доставка" -> "shipped";
            case "Получено" -> "delivered";
            default -> f;
        };
        ordersTable.getItems().setAll(allOrders.stream()
                .filter(o -> status.equals(o.getStatus())).toList());
    }


    private void setupPromosTable() {
        colPromoCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colPromoCode.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(val);
                setStyle("-fx-font-weight:bold; -fx-text-fill:#111111; -fx-font-family:monospace;");
            }
        });

        colPromoDiscount.setCellValueFactory(new PropertyValueFactory<>("discountPercent"));
        colPromoDiscount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    return;
                }
                setText("-" + val + "%");
                setStyle("-fx-text-fill:#FBB040;");
            }
        });

        colPromoUses.setCellValueFactory(new PropertyValueFactory<>("usedCount"));
        colPromoMax.setCellValueFactory(new PropertyValueFactory<>("maxUses"));

        colPromoExpires.setCellValueFactory(new PropertyValueFactory<>("expiresAt"));
        colPromoExpires.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime val, boolean empty) {
                super.updateItem(val, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                    return;
                }
                if (val == null) {
                    setText("Бессрочно");
                    setStyle("-fx-text-fill:#888888;");
                } else {
                    boolean expired = LocalDateTime.now().isAfter(val);
                    setText(val.format(DATE_FMT));
                    setStyle(expired ? "-fx-text-fill:#F87171;" : "-fx-text-fill:#4ADE80;");
                }
            }
        });

        colPromoActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        colPromoActive.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    return;
                }
                setText(val ? "Да" : "Нет");
                setStyle(val ? "-fx-text-fill:#4ADE80;" : "-fx-text-fill:#F87171;");
            }
        });

        colPromoActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Изменить");
            private final Button toggleBtn = new Button("Вкл/Выкл");
            private final Button delBtn = new Button("Удалить");
            private final HBox box = new HBox(5, editBtn, toggleBtn, delBtn);

            {
                String base = "-fx-border-radius:5; -fx-background-radius:5; -fx-cursor:hand; -fx-font-size:11px; -fx-padding:3 7;";
                editBtn.setStyle("-fx-background-color:#1E2A3A; -fx-text-fill:#60A5FA; -fx-border-color:#2D3D50;" + base);
                toggleBtn.setStyle("-fx-background-color:#2A2A1A; -fx-text-fill:#FBB040; -fx-border-color:#3A3A2A;" + base);
                delBtn.setStyle("-fx-background-color:#3A1E1E; -fx-text-fill:#F87171; -fx-border-color:#502D2D;" + base);

                editBtn.setOnAction(e -> openPromoForm(getTableView().getItems().get(getIndex())));
                toggleBtn.setOnAction(e -> {
                    PromoCode pc = getTableView().getItems().get(getIndex());
                    String err = promoSvc.toggle(pc.getId(), !pc.isActive());
                    if (err != null) AlertHelper.error("Ошибка", err);
                    else loadPromos();
                });
                delBtn.setOnAction(e -> {
                    PromoCode pc = getTableView().getItems().get(getIndex());
                    if (!AlertHelper.confirm("Удаление", "Удалить промокод «" + pc.getCode() + "»?")) return;
                    String err = promoSvc.delete(pc.getId());
                    if (err != null) AlertHelper.error("Ошибка", err);
                    else loadPromos();
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadPromos() {
        promosTable.getItems().setAll(promoSvc.getAll());
    }


    @FXML
    public void handleTabProducts() {
        switchTab(0);
    }

    @FXML
    public void handleTabOrders() {
        switchTab(1);
    }

    @FXML
    public void handleTabPromos() {
        switchTab(2);
        loadPromos();
    }

    private void switchTab(int idx) {
        tabProducts.setVisible(idx == 0);
        tabProducts.setManaged(idx == 0);
        tabOrders.setVisible(idx == 1);
        tabOrders.setManaged(idx == 1);
        tabPromos.setVisible(idx == 2);
        tabPromos.setManaged(idx == 2);
        String[] on = {"sidebar-btn", "active"}, off = {"sidebar-btn"};
        tabProductsBtn.getStyleClass().setAll(idx == 0 ? on : off);
        tabOrdersBtn.getStyleClass().setAll(idx == 1 ? on : off);
        tabPromosBtn.getStyleClass().setAll(idx == 2 ? on : off);
    }

    @FXML
    public void handleAddProduct() {
        openProductForm(null);
    }

    @FXML
    public void handleAddPromo() {
        openPromoForm(null);
    }

    private void openProductForm(Product product) {
        try {
            FXMLLoader loader = util.FxmlUtil.loader("/fxml/manager/product_form.fxml");
            Parent root = loader.load();
            ProductFormController ctrl = loader.getController();
            ctrl.setProduct(product, this::loadProducts);
            openModal(root, product == null ? "Новый товар" : "Редактировать товар", 680, 720);
        } catch (IOException e) {
            AlertHelper.error("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    private void openPromoForm(PromoCode promo) {
        try {
            FXMLLoader loader = util.FxmlUtil.loader("/fxml/manager/promo_form.fxml");
            Parent root = loader.load();
            PromoFormController ctrl = loader.getController();
            ctrl.setPromo(promo, this::loadPromos);
            openModal(root, promo == null ? "Новый промокод" : "Редактировать промокод", 440, 480);
        } catch (IOException e) {
            AlertHelper.error("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    private void openModal(Parent root, String title, double w, double h) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(SceneManager.getStage());
        modal.setTitle(title);
        Scene scene = new Scene(root);
        java.net.URL css = util.FxmlUtil.resolve("/css/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        modal.setScene(scene);
        modal.setWidth(w);
        modal.setHeight(h);
        modal.show();
    }

    @FXML
    public void handleChangeOrderStatus() {
        Order o = ordersTable.getSelectionModel().getSelectedItem();
        if (o == null) {
            AlertHelper.error("Выбор", "Выберите заказ");
            return;
        }
        String display = newStatusCombo.getValue();
        String status = switch (display) {
            case "Ожидание" -> "pending";
            case "Доставка" -> "shipped";
            case "Получено" -> "delivered";
            default -> display;
        };
        String err = orderSvc.updateStatus(o.getId(), status);
        if (err != null) AlertHelper.error("Ошибка", err);
        else {
            loadOrders();
            AlertHelper.info("Готово", "Заказ #" + o.getId() + " → " + display);
        }
    }

    @FXML
    public void handleBack() {
        SceneManager.showCatalog();
    }

    @FXML
    public void handleLogout() {
        new service.AuthService().logout();
        SceneManager.showLogin();
    }
}
