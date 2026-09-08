package edu.du.iit.cms.ui;

import edu.du.iit.cms.AppServices;
import edu.du.iit.cms.domain.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public final class LoginView extends VBox {
    public LoginView(AppServices services, Consumer<User> onLogin) {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(30));

        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setMaxWidth(420);

        Label title = UiSupport.title("IIT Course Management System");
        Label subtitle = new Label("Sign in with an Administrator, Teacher, or Student account.");
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Label message = new Label();
        message.getStyleClass().add("status-message");
        message.setWrapText(true);
        Button login = new Button("Login");
        login.setMaxWidth(Double.MAX_VALUE);

        Runnable submit = () -> {
            try {
                message.setText("");
                onLogin.accept(services.auth().login(username.getText(), password.getText()));
            } catch (RuntimeException exception) {
                message.setText(exception.getMessage());
            }
        };
        login.setOnAction(event -> submit.run());
        password.setOnAction(event -> submit.run());

        Label accounts = new Label("Demo: admin/admin123 | teacher1/teacher123 | student1/student123");
        accounts.setStyle("-fx-text-fill: #617186; -fx-font-size: 11px;");
        accounts.setWrapText(true);

        card.getChildren().addAll(title, subtitle, new Label("Username"), username,
                new Label("Password"), password, message, login, accounts);
        getChildren().add(card);
    }
}

