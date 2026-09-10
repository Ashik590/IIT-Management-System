package edu.du.iit.cms.ui;

import edu.du.iit.cms.AppServices;
import edu.du.iit.cms.domain.AssessmentComponent;
import edu.du.iit.cms.domain.AttendanceSummary;
import edu.du.iit.cms.domain.Course;
import edu.du.iit.cms.domain.ResourceItem;
import edu.du.iit.cms.domain.StudentAcademicSummary;
import edu.du.iit.cms.domain.User;
import edu.du.iit.cms.service.ValidationException;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;

public final class StudentDashboard extends BorderPane {
    private final AppServices services;
    private final User student;
    private final ListView<Course> courses = new ListView<>();
    private final TextArea overview = new TextArea();
    private final ListView<String> attendance = new ListView<>();
    private final ListView<String> evaluation = new ListView<>();
    private final ListView<ResourceItem> resources = new ListView<>();

    public StudentDashboard(AppServices services, User student, Runnable logout) {
        this.services = services;
        this.student = student;
        setTop(UiSupport.header("Student Dashboard", student.fullName(), logout));

        VBox left = new VBox(10, UiSupport.sectionTitle("My courses"), courses);
        left.getStyleClass().add("card");
        left.setPrefWidth(350);
        courses.setPrefHeight(620);
        setLeft(left);
        BorderPane.setMargin(left, new Insets(16, 8, 16, 16));

        overview.setEditable(false);
        overview.setWrapText(true);
        attendance.setPrefHeight(480);
        evaluation.setPrefHeight(480);
        resources.setPrefHeight(420);

        Button openResource = new Button("Open selected resource");
        openResource.setOnAction(event -> UiSupport.runAction(null, () -> openSelectedResource()));

        TabPane tabs = new TabPane(
                tab("Overview", overview),
                tab("Attendance", attendance),
                tab("Continuous Evaluation", evaluation),
                tab("Resources", new VBox(10, resources, openResource))
        );
        setCenter(tabs);
        BorderPane.setMargin(tabs, new Insets(16, 16, 16, 8));

        courses.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, value) -> refreshCourse());
        courses.getItems().setAll(services.courses().coursesForStudent(student.id()));
        if (!courses.getItems().isEmpty()) {
            courses.getSelectionModel().selectFirst();
        }
    }

    private void refreshCourse() {
        Course course = courses.getSelectionModel().getSelectedItem();
        overview.clear();
        attendance.getItems().clear();
        evaluation.getItems().clear();
        resources.getItems().clear();
        if (course == null) {
            return;
        }

        StudentAcademicSummary summary = services.reporting().studentSummary(course.id(), student.id());
        StringBuilder text = new StringBuilder();
        text.append(course.courseCode()).append(" - ").append(course.title()).append('\n')
                .append("Type: ").append(course.courseType()).append('\n')
                .append("Session: ").append(course.academicSession()).append('\n')
                .append("Semester: ").append(course.semester()).append('\n')
                .append("Course status: ").append(course.status()).append('\n')
                .append("Enrollment status: ").append(summary.enrollmentStatus()).append('\n')
                .append("CE structure: ").append(course.ceStatus()).append('\n')
                .append("Attendance: ").append(format(summary.attendancePercentage())).append('%').append('\n')
                .append("Current CE: ").append(format(summary.ceMark())).append(" / ")
                .append(course.courseType().ceMarks());
        if (summary.finalExamMark() != null) {
            text.append("\nFinal examination: ").append(format(summary.finalExamMark())).append(" / ")
                    .append(course.courseType().finalExamMarks());
        }
        if (summary.totalMark() != null) {
            text.append("\nTotal: ").append(format(summary.totalMark())).append(" / 100");
        }
        overview.setText(text.toString());

        for (AttendanceSummary row : services.attendance().studentHistory(course.id(), student.id())) {
            String title = row.title() == null ? "Class" : row.title();
            attendance.getItems().add(row.classDate() + " | " + title + " | " + row.status());
        }
        if (attendance.getItems().isEmpty()) {
            attendance.getItems().add("Attendance has not yet been recorded.");
        }

        for (AssessmentComponent component : services.evaluation().components(course.id())) {
            Double mark = services.evaluation().mark(component.id(), student.id());
            evaluation.getItems().add(component.title() + " | obtained " + format(mark)
                    + " / " + format(component.maximumMark()) + " | weight "
                    + format(component.weightPercentage()) + "%");
        }
        evaluation.getItems().add("CE total: " + format(summary.ceMark()) + " / "
                + course.courseType().ceMarks()
                + (course.ceStatus().name().equals("FINALIZED") ? "" : " (provisional)"));

        resources.getItems().setAll(services.resources().resources(course.id(), student.id()));
    }

    private void openSelectedResource() {
        ResourceItem selected = resources.getSelectionModel().getSelectedItem();
        if (selected == null) {
            throw new ValidationException("Select a resource.");
        }
        if (!Files.isRegularFile(selected.storedPath())) {
            throw new ValidationException("The stored file is missing or inaccessible.");
        }
        if (!Desktop.isDesktopSupported()) {
            throw new ValidationException("Opening files is not supported on this computer.");
        }
        try {
            Desktop.getDesktop().open(selected.storedPath().toFile());
        } catch (IOException exception) {
            throw new ValidationException("Could not open the resource: " + exception.getMessage(), exception);
        }
    }

    private Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private String format(Double value) {
        return value == null ? "—" : String.format("%.2f", value);
    }
}
