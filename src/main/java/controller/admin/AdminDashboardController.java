package controller.admin;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Review;
import model.User;
import service.ReviewService;
import service.UserService;
import util.AlertHelper;
import util.SceneManager;
import util.Session;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML
    private Label adminNameLabel;
    @FXML
    private Button tabUsersBtn, tabReviewsBtn, tabLogsBtn;
    @FXML
    private VBox tabUsers, tabReviews, tabLogs;

    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, Long> colUserId;
    @FXML
    private TableColumn<User, String> colUsername;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, Boolean> colLocked;
    @FXML
    private TableColumn<User, LocalDateTime> colCreated;
    @FXML
    private TableColumn<User, Void> colUserActions;
    @FXML
    private TextField userSearchField;

    @FXML
    private TableView<Review> reviewsTable;
    @FXML
    private TableColumn<Review, Long> colRevId;
    @FXML
    private TableColumn<Review, String> colRevUser;
    @FXML
    private TableColumn<Review, String> colRevProduct;
    @FXML
    private TableColumn<Review, Integer> colRevRating;
    @FXML
    private TableColumn<Review, String> colRevComment;
    @FXML
    private TableColumn<Review, Void> colRevActions;

    @FXML
    private TableView<Object[]> logsTable;
    @FXML
    private TableColumn<Object[], Long> colLogId;
    @FXML
    private TableColumn<Object[], String> colLogTime;
    @FXML
    private TableColumn<Object[], String> colLogUser;
    @FXML
    private TableColumn<Object[], String> colLogAction;
    @FXML
    private TableColumn<Object[], String> colLogTable;
    @FXML
    private TableColumn<Object[], String> colLogDetails;

    private final UserService userSvc = new UserService();
    private final ReviewService reviewSvc = new ReviewService();
    private List<User> allUsers;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        adminNameLabel.setText(Session.getInstance().getCurrentUser().getFullName());
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        reviewsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        logsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupUsersTable();
        setupReviewsTable();
        setupLogsTable();
        loadAll();
        userSearchField.textProperty().addListener((obs, o, n) -> filterUsers(n));
    }

    private void setupUsersTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roleDisplay"));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(val);
                setStyle(val.contains("Адм") ? "-fx-text-fill:#F87171;"
                        : val.contains("Мен") ? "-fx-text-fill:#FBB040;"
                        : "-fx-text-fill:#9090B0;");
            }
        });
        colLocked.setCellValueFactory(new PropertyValueFactory<>("accountLocked"));
        colLocked.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    return;
                }
                setText(val ? "Да" : "—");
                setStyle(val ? "-fx-text-fill:#F87171;" : "-fx-text-fill:#60608A;");
            }
        });
        colCreated.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colCreated.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : val.toString().substring(0, 16).replace("T", " "));
            }
        });
        colUserActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnUnblock = new Button("Разблокировать");
            private final Button btnBlock = new Button("Заблокировать");
            private final HBox box = new HBox(6, btnUnblock, btnBlock);

            {
                String base = "-fx-border-radius:5; -fx-background-radius:5; -fx-cursor:hand; -fx-font-size:11px; -fx-padding:3 7;";
                btnUnblock.setStyle("-fx-background-color:#1A3A2A; -fx-text-fill:#4ADE80; -fx-border-color:#2A5A3A;" + base);
                btnBlock.setStyle("-fx-background-color:#3A1E1E; -fx-text-fill:#F87171; -fx-border-color:#502D2D;" + base);
                btnUnblock.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    String err = userSvc.unblockUser(u.getId());
                    if (err != null) AlertHelper.error("Ошибка", err);
                    else {
                        AlertHelper.info("Готово", "Пользователь разблокирован");
                        loadAll();
                    }
                });
                btnBlock.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (!AlertHelper.confirm("Блокировка", "Заблокировать " + u.getUsername() + "?")) return;
                    String err = userSvc.blockUser(u.getId());
                    if (err != null) AlertHelper.error("Ошибка", err);
                    else {
                        AlertHelper.info("Готово", "Пользователь заблокирован");
                        loadAll();
                    }
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupReviewsTable() {
        colRevId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRevUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRevProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colRevRating.setCellValueFactory(new PropertyValueFactory<>("rating"));
        colRevRating.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    return;
                }
                setText("*".repeat(val) + " (" + val + "/5)");
                setStyle("-fx-text-fill:#FBB040;");
            }
        });
        colRevComment.setCellValueFactory(new PropertyValueFactory<>("comment"));
        colRevActions.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Удалить");

            {
                btn.setStyle("-fx-background-color:#3A1E1E; -fx-text-fill:#F87171; -fx-border-color:#502D2D;" +
                        "-fx-border-radius:5; -fx-background-radius:5; -fx-cursor:hand; -fx-font-size:11px; -fx-padding:3 10;");
                btn.setOnAction(e -> {
                    Review r = getTableView().getItems().get(getIndex());
                    if (!AlertHelper.confirm("Удаление", "Удалить отзыв от " + r.getUsername() + "?")) return;
                    reviewSvc.reject(r.getId());
                    reviewsTable.getItems().setAll(reviewSvc.getAll());
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void setupLogsTable() {
        colLogId.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>((Long) d.getValue()[0]));
        colLogTime.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(fmtTime((String) d.getValue()[5])));
        colLogUser.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue()[1] != null ? (String) d.getValue()[1] : "Система"));
        colLogAction.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(xlAction((String) d.getValue()[2])));
        colLogAction.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(val);
                if (val.contains("Вход")) setStyle("-fx-text-fill:#4ADE80;");
                else if (val.contains("Выход")) setStyle("-fx-text-fill:#60A5FA;");
                else if (val.contains("Блок")) setStyle("-fx-text-fill:#F87171;");
                else if (val.contains("Удал")) setStyle("-fx-text-fill:#F87171;");
                else if (val.contains("Создан")) setStyle("-fx-text-fill:#C6FF00;");
                else setStyle("-fx-text-fill:#9090B0;");
            }
        });
        colLogTable.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(xlTable((String) d.getValue()[3])));
        colLogDetails.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[4]));
    }

    private String xlAction(String a) {
        if (a == null) return "";
        return switch (a.toUpperCase()) {
            case "LOGIN" -> "Вход в систему";
            case "LOGOUT" -> "Выход из системы";
            case "REGISTER" -> "Регистрация";
            case "ORDER_PLACED" -> "Создание заказа";
            case "ORDER_STATUS_UPDATED" -> "Смена статуса";
            case "PRODUCT_CREATED" -> "Добавление товара";
            case "PRODUCT_UPDATED" -> "Редактирование товара";
            case "PRODUCT_DELETED" -> "Удаление товара";
            case "REVIEW_SUBMITTED" -> "Добавление отзыва";
            case "USER_BLOCKED" -> "Блокировка пользователя";
            case "USER_UNBLOCKED" -> "Разблокировка";
            case "USER_DEACTIVATED" -> "Деактивация пользователя";
            default -> a;
        };
    }

    private String xlTable(String t) {
        if (t == null) return "";
        return switch (t.toLowerCase()) {
            case "users" -> "Пользователи";
            case "products" -> "Товары";
            case "orders" -> "Заказы";
            case "reviews" -> "Отзывы";
            case "payment_cards" -> "Карты оплаты";
            default -> t;
        };
    }

    private String fmtTime(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.replace("T", " ");
        return s.length() > 16 ? s.substring(0, 16) : s;
    }

    private void loadAll() {
        allUsers = userSvc.getAll();
        usersTable.getItems().setAll(allUsers);
        reviewsTable.getItems().setAll(reviewSvc.getAll());
    }

    private void filterUsers(String query) {
        if (query == null || query.isBlank()) {
            usersTable.getItems().setAll(allUsers);
            return;
        }
        String q = query.toLowerCase();
        usersTable.getItems().setAll(allUsers.stream()
                .filter(u -> u.getUsername().toLowerCase().contains(q)
                        || u.getEmail().toLowerCase().contains(q))
                .toList());
    }

    @FXML
    public void handleTabUsers() {
        switchTab(0);
    }

    @FXML
    public void handleTabReviews() {
        switchTab(1);
    }

    @FXML
    public void handleTabLogs() {
        switchTab(2);
        logsTable.getItems().setAll(userSvc.getLogs());
    }

    private void switchTab(int idx) {
        tabUsers.setVisible(idx == 0);
        tabUsers.setManaged(idx == 0);
        tabReviews.setVisible(idx == 1);
        tabReviews.setManaged(idx == 1);
        tabLogs.setVisible(idx == 2);
        tabLogs.setManaged(idx == 2);
        String[] on = {"sidebar-btn", "active"}, off = {"sidebar-btn"};
        tabUsersBtn.getStyleClass().setAll(idx == 0 ? on : off);
        tabReviewsBtn.getStyleClass().setAll(idx == 1 ? on : off);
        tabLogsBtn.getStyleClass().setAll(idx == 2 ? on : off);
    }

    @FXML
    public void handleRefreshUsers() {
        loadAll();
    }

    @FXML
    public void handleRefreshReviews() {
        reviewsTable.getItems().setAll(reviewSvc.getAll());
    }

    @FXML
    public void handleRefreshLogs() {
        logsTable.getItems().setAll(userSvc.getLogs());
    }

    @FXML
    public void handleLogout() {
        new service.AuthService().logout();
        SceneManager.showLogin();
    }
}
