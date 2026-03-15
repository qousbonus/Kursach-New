package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import service.AuthService;
import util.AlertHelper;
import util.SceneManager;

public class RegisterController {

    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private final AuthService auth = new AuthService();

    @FXML
    public void handleRegister() {
        String err = auth.register(
                usernameField.getText(),
                emailField.getText(),
                passwordField.getText()
        );
        if (err != null) {
            errorLabel.setText(err);
        } else {
            AlertHelper.info("Регистрация успешна", "Аккаунт создан.");
            SceneManager.showLogin();
        }
    }

    @FXML
    public void handleBack() {
        SceneManager.showLogin();
    }
}
