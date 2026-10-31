package app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import util.SceneManager;
import util.ThemeManager;
import util.OrderTrackingFeature;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SPORT NUTRITION STORE");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.setMaximized(true);
        SceneManager.init(primaryStage);
        ThemeManager.initialize(primaryStage);
        primaryStage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) Platform.runLater(() -> {
                if (primaryStage.getScene() != newScene) return;
                newScene.getRoot().applyCss();
                newScene.getRoot().layout();
                OrderTrackingFeature.installManager(newScene.getRoot());
            });
        });

        new Thread(() -> {
            try { new service.ReviewService().syncAllProductStats(); }
            catch (Exception ignored) {}
        }).start();

        SceneManager.showLogin();
    }
    public static void main(String[] args) { launch(args); }
}
