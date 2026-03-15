module com.sportnutrition {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.google.gson;

    opens app                to javafx.graphics, javafx.fxml;
    opens controller         to javafx.fxml;
    opens controller.manager to javafx.fxml;
    opens controller.admin   to javafx.fxml;
    opens model              to javafx.base, javafx.fxml;
    opens util               to javafx.fxml;

    exports app;
    exports controller;
    exports controller.manager;
    exports controller.admin;
    exports model;
    exports service;
    exports util;
    exports repository;
}
