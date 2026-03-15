package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import service.AuthService;
import util.SceneManager;
import util.Session;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleLogin() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (u.isEmpty() || p.isEmpty()) {
            errorLabel.setText("Заполните все поля");
            return;
        }
        String err = authService.login(u, p);
        if (err != null) {
            errorLabel.setText(err);
            return;
        }
        Session s = Session.getInstance();
        if (s.isAdmin()) SceneManager.showAdmin();
        else if (s.isManager()) SceneManager.showManager();
        else SceneManager.showCatalog();
    }

    @FXML
    public void handleGuestEntry() {
        SceneManager.showCatalog();
    }

    @FXML
    public void handleRegister() {
        SceneManager.showRegister();
    }
}
