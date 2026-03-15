package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import model.Category;
import model.Product;
import service.ProductService;
import util.SceneManager;
import util.Session;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CatalogController implements Initializable {

    @FXML
    private Button cartCountLabel;
    @FXML
    private Button backToManagerBtn;
    @FXML
    private Button ordersBtn;
    @FXML
    private TextField searchField;
    @FXML
    private HBox categoryBar;
    @FXML
    private TilePane productGrid;
    @FXML
    private Label productCountLabel;
    @FXML
    private Label emptyGridLabel;

    private final ProductService svc = new ProductService();
    private List<Product> allProducts;
    private long selectedCategoryId = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        boolean manager = isManagerMode();

        if (cartCountLabel != null) {
            cartCountLabel.setVisible(!manager);
            cartCountLabel.setManaged(!manager);
        }
        if (ordersBtn != null) {
            ordersBtn.setVisible(!manager);
            ordersBtn.setManaged(!manager);
        }
        if (backToManagerBtn != null) {
            backToManagerBtn.setVisible(manager);
            backToManagerBtn.setManaged(manager);
        }

        if (!manager) updateCartLabel();
        loadCategories();
        loadProducts();
        searchField.textProperty().addListener((obs, o, n) -> filterBySearch(n));
        productGrid.widthProperty().addListener((obs, o, w) -> updateTileSize(w.doubleValue()));
    }

    public boolean isManagerMode() {
        Session s = Session.getInstance();
        return s.isLoggedIn() && "manager".equals(s.getCurrentUser().getRole());
    }

    private void updateTileSize(double w) {
        double gap = 20;
        int cols = Math.max(1, (int) ((w + gap) / (200 + gap)));
        productGrid.setPrefColumns(cols);
        productGrid.setPrefTileWidth((w - gap * (cols - 1)) / cols);
    }

    private void loadCategories() {
        categoryBar.getChildren().clear();
        Button allBtn = new Button("Все");
        allBtn.getStyleClass().addAll("category-btn", "active");
        allBtn.setOnAction(e -> {
            selectedCategoryId = 0;
            clearActiveCategory();
            allBtn.getStyleClass().add("active");
            loadProducts();
        });
        categoryBar.getChildren().add(allBtn);

        for (Category cat : svc.getCategories()) {
            Button btn = new Button(cat.getDisplayName());
            btn.getStyleClass().add("category-btn");
            btn.setOnAction(e -> {
                selectedCategoryId = cat.getId();
                clearActiveCategory();
                btn.getStyleClass().add("active");
                allProducts = svc.getByCategory(cat.getId());
                renderProducts(allProducts);
            });
            categoryBar.getChildren().add(btn);
        }
    }

    private void clearActiveCategory() {
        categoryBar.getChildren().forEach(n -> n.getStyleClass().remove("active"));
    }

    private void loadProducts() {
        allProducts = svc.getAvailable();
        renderProducts(allProducts);
    }

    private void filterBySearch(String query) {
        if (query == null || query.isBlank()) {
            if (selectedCategoryId == 0) loadProducts();
            else {
                allProducts = svc.getByCategory(selectedCategoryId);
                renderProducts(allProducts);
            }
            return;
        }
        renderProducts(svc.search(query));
    }

    private void renderProducts(List<Product> products) {
        productGrid.getChildren().clear();
        productCountLabel.setText("Найдено: " + products.size() + " товаров");
        boolean empty = products.isEmpty();
        if (emptyGridLabel != null) {
            emptyGridLabel.setVisible(empty);
            emptyGridLabel.setManaged(empty);
        }
        if (empty) return;

        for (Product p : products) {
            try {
                FXMLLoader loader = util.FxmlUtil.loader("/fxml/product_card.fxml");
                Parent card = loader.load();
                ProductCardController ctrl = loader.getController();
                ctrl.setProduct(p, this);
                productGrid.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateCartLabel() {
        if (cartCountLabel == null) return;
        int cnt = Session.getInstance().getCartCount();
        cartCountLabel.setText(cnt > 0 ? "Корзина (" + cnt + ")" : "Корзина");
    }

    public void openProductDetail(Product product) {
        SceneManager.showProductDetail(product, this);
    }

    @FXML
    public void handleBackToManager() {
        SceneManager.showManagerDashboard();
    }

    @FXML
    public void handleCart() {
        if (Session.getInstance().isGuest()) SceneManager.showLogin();
        else SceneManager.showCart();
    }

    @FXML
    public void handleOrders() {
        if (Session.getInstance().isLoggedIn()) SceneManager.showOrders();
        else SceneManager.showLogin();
    }

    @FXML
    public void handleProfile() {
        if (Session.getInstance().isLoggedIn()) SceneManager.showProfile();
        else SceneManager.showLogin();
    }

    @FXML
    public void handleLogout() {
        new service.AuthService().logout();
        SceneManager.showLogin();
    }
}
