package app;

import javafx.application.Application;
import javafx.stage.Stage;
import util.SceneManager;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SPORT NUTRITION STORE");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.setMaximized(true);
        SceneManager.init(primaryStage);

        new Thread(() -> {
            try { new service.ReviewService().syncAllProductStats(); }
            catch (Exception ignored) {}
        }).start();

        SceneManager.showLogin();
    }
    public static void main(String[] args) { launch(args); }
}
