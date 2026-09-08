package edu.du.iit.cms;

import edu.du.iit.cms.domain.Role;
import edu.du.iit.cms.domain.User;
import edu.du.iit.cms.ui.AdminDashboard;
import edu.du.iit.cms.ui.LoginView;
import edu.du.iit.cms.ui.StudentDashboard;
import edu.du.iit.cms.ui.TeacherDashboard;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;

public final class IitCourseManagementApp extends Application {
    private AppServices services;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        services = new AppServices(Path.of(System.getProperty("cms.data.dir", "data")), true);
        stage.setTitle("IIT Course Management System");
        stage.setMinWidth(1000);
        stage.setMinHeight(680);
        showLogin();
        stage.show();
    }

    private void showLogin() {
        show(new LoginView(services, this::showDashboard), 1000, 680);
    }

    private void showDashboard(User user) {
        Parent dashboard;
        if (user.role() == Role.ADMIN) {
            dashboard = new AdminDashboard(services, user, this::showLogin);
        } else if (user.role() == Role.TEACHER) {
            dashboard = new TeacherDashboard(services, user, this::showLogin);
        } else {
            dashboard = new StudentDashboard(services, user, this::showLogin);
        }
        show(dashboard, 1200, 780);
    }

    private void show(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        String stylesheet = IitCourseManagementApp.class.getResource("/style.css").toExternalForm();
        scene.getStylesheets().add(stylesheet);
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
