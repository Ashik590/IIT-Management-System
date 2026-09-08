package edu.du.iit.cms.ui;

import edu.du.iit.cms.service.ValidationException;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public final class UiSupport {
    private UiSupport() {
    }

    public static Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("app-title");
        return label;
    }

    public static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    public static HBox header(String title, String subtitle, Runnable logout) {
        Label heading = UiSupport.title(title);
        Label detail = new Label(subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        javafx.scene.control.Button logoutButton = new javafx.scene.control.Button("Logout");
        logoutButton.getStyleClass().add("secondary");
        logoutButton.setOnAction(event -> logout.run());
        HBox header = new HBox(12, heading, detail, spacer, logoutButton);
        header.setPadding(new Insets(14));
        header.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #d8e0ea transparent;");
        return header;
    }

    public static void runAction(String successMessage, Runnable action) {
        try {
            action.run();
            if (successMessage != null && !successMessage.isBlank()) {
                showInfo(successMessage);
            }
        } catch (ValidationException exception) {
            showError(exception.getMessage());
        } catch (RuntimeException exception) {
            showError(rootMessage(exception));
        }
    }

    public static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Please confirm");
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    public static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText("Completed");
        alert.showAndWait();
    }

    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Operation could not be completed");
        alert.showAndWait();
    }

    public static double number(String text, String field) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception exception) {
            throw new ValidationException(field + " must be a number.");
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? throwable.getClass().getSimpleName() : current.getMessage();
    }
}

