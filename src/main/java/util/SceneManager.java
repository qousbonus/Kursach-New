package util;

import controller.CatalogController;
import controller.ProductDetailController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Product;

import java.io.IOException;
import java.net.URL;

public class SceneManager {

    private static Stage stage;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static Stage getStage() {
        return stage;
    }

    private static void applyScene(Scene scene) {
        boolean wasMaximized = stage.isMaximized();
        double w = wasMaximized ? 1920 : stage.getWidth();
        double h = wasMaximized ? 1080 : stage.getHeight();
        double x = stage.getX();
        double y = stage.getY();

        if (wasMaximized) {
            stage.setMaximized(false);
        }

        stage.setScene(scene);
        stage.setWidth(w);
        stage.setHeight(h);

        if (!wasMaximized) {
            stage.setX(x);
            stage.setY(y);
        }

        stage.show();

        if (wasMaximized) {
            Platform.runLater(() -> {
                stage.setMaximized(true);
                Platform.runLater(() -> {
                    scene.getRoot().applyCss();
                    scene.getRoot().layout();
                });
            });
        } else {
            Platform.runLater(() -> {
                scene.getRoot().applyCss();
                scene.getRoot().layout();
            });
        }
    }

    private static Scene buildScene(FXMLLoader loader) throws IOException {
        Parent root = loader.load();
        Scene scene = new Scene(root);
        URL cssUrl = FxmlUtil.resolve("/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        return scene;
    }

    public static void show(String fxmlPath) {
        try {
            FXMLLoader loader = FxmlUtil.loader(fxmlPath);
            applyScene(buildScene(loader));
        } catch (IOException e) {
            throw new RuntimeException("Cannot load FXML: " + fxmlPath, e);
        }
    }

    public static <T> T showAndGetController(String fxmlPath) {
        try {
            FXMLLoader loader = FxmlUtil.loader(fxmlPath);
            applyScene(buildScene(loader));
            return loader.getController();
        } catch (IOException e) {
            throw new RuntimeException("Cannot load FXML: " + fxmlPath, e);
        }
    }

    public static void showProductDetail(Product product, CatalogController catalogController) {
        try {
            FXMLLoader loader = FxmlUtil.loader("/fxml/product_detail.fxml");
            Parent root = loader.load();
            ProductDetailController ctrl = loader.getController();
            ctrl.setProduct(product, catalogController);
            Scene scene = new Scene(root);
            URL cssUrl = FxmlUtil.resolve("/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            applyScene(scene);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load product detail", e);
        }
    }

    public static void showLogin() {
        show("/fxml/login.fxml");
    }

    public static void showRegister() {
        show("/fxml/register.fxml");
    }

    public static void showCatalog() {
        show("/fxml/catalog.fxml");
    }

    public static void showCart() {
        show("/fxml/cart.fxml");
    }

    public static void showOrders() {
        show("/fxml/orders.fxml");
    }

    public static void showProfile() {
        show("/fxml/profile.fxml");
    }

    public static void showManager() {
        show("/fxml/manager/dashboard.fxml");
    }

    public static void showManagerDashboard() {
        show("/fxml/manager/dashboard.fxml");
    }

    public static void showAdmin() {
        show("/fxml/admin/dashboard.fxml");
    }
}
