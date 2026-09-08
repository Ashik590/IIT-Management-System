package edu.du.iit.cms.ui;

import edu.du.iit.cms.AppServices;
import edu.du.iit.cms.domain.AssessmentComponent;
import edu.du.iit.cms.domain.AttendanceStatus;
import edu.du.iit.cms.domain.Course;
import edu.du.iit.cms.domain.CourseStudent;
import edu.du.iit.cms.domain.ResourceItem;
import edu.du.iit.cms.domain.StudentAcademicSummary;
import edu.du.iit.cms.domain.User;
import edu.du.iit.cms.service.ValidationException;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TeacherDashboard extends BorderPane {
    private final AppServices services;
    private final User teacher;
    private final ListView<Course> courseList = new ListView<>();
    private final TextArea summary = new TextArea();
    private final VBox attendanceRows = new VBox(7);
    private final Map<Long, CheckBox> attendanceChecks = new LinkedHashMap<>();
    private final ListView<AssessmentComponent> components = new ListView<>();
    private final ComboBox<CourseStudent> markStudent = new ComboBox<>();
    private final TextField markField = field("Obtained mark");
    private final TextField componentTitle = field("Quiz 1");
    private final TextField componentWeight = field("Weight %");
    private final TextField componentMaximum = field("Maximum mark");
    private final TextField updatedWeight = field("New weight %");
    private final Label weightStatus = new Label("Total weight: —");
    private final ListView<ResourceItem> resources = new ListView<>();

    public TeacherDashboard(AppServices services, User teacher, Runnable logout) {
        this.services = services;
        this.teacher = teacher;
        setTop(UiSupport.header("Teacher Dashboard", teacher.fullName(), logout));
        setPadding(new Insets(0));

        VBox courseBox = new VBox(10, UiSupport.sectionTitle("Assigned courses"), courseList);
        courseBox.getStyleClass().add("card");
        courseBox.setPrefWidth(330);
        courseBox.setPadding(new Insets(14));
        courseList.setPrefHeight(620);
        setLeft(courseBox);
        BorderPane.setMargin(courseBox, new Insets(16, 8, 16, 16));

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                tab("Overview", overviewPane()),
                tab("Attendance", attendancePane()),
                tab("Continuous Evaluation", evaluationPane()),
                tab("Resources", resourcePane())
        );
        setCenter(tabs);
        BorderPane.setMargin(tabs, new Insets(16, 16, 16, 8));

        courseList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, value) -> refreshSelectedCourse());
        refreshCourses(null);
    }

    private BorderPane overviewPane() {
        summary.setEditable(false);
        summary.setWrapText(false);
        BorderPane pane = new BorderPane(summary);
        pane.setPadding(new Insets(14));
        return pane;
    }

    private ScrollPane attendancePane() {
        DatePicker date = new DatePicker(LocalDate.now());
        TextField title = field("Optional session title");
        Button allPresent = new Button("Mark all Present");
        allPresent.getStyleClass().add("secondary");
        allPresent.setOnAction(event -> attendanceChecks.values().forEach(box -> box.setSelected(true)));
        Button save = new Button("Submit attendance session");
        save.setOnAction(event -> UiSupport.runAction("Attendance session saved.", () -> {
            Course course = selectedCourse();
            Map<Long, AttendanceStatus> values = new LinkedHashMap<>();
            attendanceChecks.forEach((studentId, check) -> values.put(studentId,
                    check.isSelected() ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT));
            services.attendance().createSession(teacher.id(), course.id(), date.getValue(), title.getText(), values);
            title.clear();
            refreshSelectedCourse();
        }));

        VBox content = card("New attendance session", new HBox(8, new Label("Date"), date, title),
                allPresent, attendanceRows, save);
        HBox.setHgrow(title, Priority.ALWAYS);
        return scroll(content);
    }

    private ScrollPane evaluationPane() {
        components.setPrefHeight(180);
        markStudent.setMaxWidth(Double.MAX_VALUE);

        Button add = new Button("Add component");
        add.setOnAction(event -> UiSupport.runAction("Assessment component added.", () -> {
            Course course = selectedCourse();
            services.evaluation().addComponent(teacher.id(), course.id(), componentTitle.getText(),
                    UiSupport.number(componentWeight.getText(), "Weight"),
                    UiSupport.number(componentMaximum.getText(), "Maximum mark"));
            componentTitle.clear();
            componentWeight.clear();
            componentMaximum.clear();
            refreshCourses(course.id());
        }));

        Button changeWeight = new Button("Change selected weight");
        changeWeight.getStyleClass().add("secondary");
        changeWeight.setOnAction(event -> UiSupport.runAction("Weight updated; CE structure returned to Draft.", () -> {
            Course course = selectedCourse();
            AssessmentComponent component = require(components.getSelectionModel().getSelectedItem(),
                    "Select an assessment component.");
            services.evaluation().updateWeight(teacher.id(), course.id(), component.id(),
                    UiSupport.number(updatedWeight.getText(), "New weight"));
            updatedWeight.clear();
            refreshCourses(course.id());
        }));

        Button deleteComponent = new Button("Delete selected component");
        deleteComponent.getStyleClass().add("danger");
        deleteComponent.setOnAction(event -> {
            Course course;
            AssessmentComponent component;
            try {
                course = selectedCourse();
                component = require(components.getSelectionModel().getSelectedItem(),
                        "Select an assessment component.");
            } catch (ValidationException exception) {
                UiSupport.showError(exception.getMessage());
                return;
            }
            if (UiSupport.confirm("Delete assessment component '" + component.title()
                    + "'? Its Student marks will also be deleted.")) {
                UiSupport.runAction("Assessment component deleted; CE structure returned to Draft.", () -> {
                    services.evaluation().deleteComponent(teacher.id(), course.id(), component.id());
                    refreshCourses(course.id());
                });
            }
        });

        Button finalize = new Button("Finalize CE structure");
        finalize.setOnAction(event -> UiSupport.runAction("CE structure finalized.", () -> {
            Course course = selectedCourse();
            services.evaluation().finalizeStructure(teacher.id(), course.id());
            refreshCourses(course.id());
        }));

        components.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, value) -> loadMark());
        markStudent.valueProperty().addListener((observable, oldValue, value) -> loadMark());

        Button saveMark = new Button("Save Student mark");
        saveMark.setOnAction(event -> UiSupport.runAction("Assessment mark saved.", () -> {
            Course course = selectedCourse();
            AssessmentComponent component = require(components.getSelectionModel().getSelectedItem(),
                    "Select an assessment component.");
            CourseStudent student = require(markStudent.getValue(), "Select a Student.");
            services.evaluation().saveMark(teacher.id(), course.id(), component.id(), student.studentId(),
                    UiSupport.number(markField.getText(), "Obtained mark"));
            refreshSelectedCourse();
        }));

        GridPane createForm = grid();
        addRow(createForm, 0, "Title", componentTitle);
        addRow(createForm, 1, "Weight %", componentWeight);
        addRow(createForm, 2, "Maximum mark", componentMaximum);

        VBox content = card("Continuous Evaluation", weightStatus, components,
                UiSupport.sectionTitle("Add component"), createForm, add,
                UiSupport.sectionTitle("Edit component"), new HBox(8, updatedWeight, changeWeight),
                deleteComponent, finalize,
                UiSupport.sectionTitle("Enter or update mark"), markStudent, markField, saveMark);
        HBox.setHgrow(updatedWeight, Priority.ALWAYS);
        return scroll(content);
    }

    private ScrollPane resourcePane() {
        resources.setPrefHeight(330);
        Button upload = new Button("Choose and upload resource");
        upload.setOnAction(event -> {
            Course course;
            try {
                course = selectedCourse();
            } catch (ValidationException exception) {
                UiSupport.showError(exception.getMessage());
                return;
            }
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select course resource");
            File selected = chooser.showOpenDialog(getScene().getWindow());
            if (selected != null) {
                UiSupport.runAction("Resource uploaded.", () -> {
                    services.resources().upload(teacher.id(), course.id(), selected.toPath());
                    refreshResources(course);
                });
            }
        });
        VBox content = card("Course resources", new Label("Maximum file size: 20 MB"), resources, upload);
        return scroll(content);
    }

    private void refreshCourses(Long preserveCourseId) {
        courseList.getItems().setAll(services.courses().coursesForTeacher(teacher.id()));
        if (preserveCourseId != null) {
            courseList.getItems().stream().filter(course -> course.id() == preserveCourseId)
                    .findFirst().ifPresent(courseList.getSelectionModel()::select);
        } else if (!courseList.getItems().isEmpty()) {
            courseList.getSelectionModel().selectFirst();
        }
    }

    private void refreshSelectedCourse() {
        Course course = courseList.getSelectionModel().getSelectedItem();
        attendanceRows.getChildren().clear();
        attendanceChecks.clear();
        components.getItems().clear();
        markStudent.getItems().clear();
        resources.getItems().clear();
        summary.clear();
        if (course == null) {
            return;
        }

        var roster = services.courses().students(course.id());
        StringBuilder overview = new StringBuilder();
        overview.append(course).append('\n')
                .append("Session: ").append(course.academicSession()).append(" | Semester: ")
                .append(course.semester()).append(" | Credit: ").append(course.credit()).append('\n')
                .append("CE status: ").append(course.ceStatus()).append("\n\n")
                .append("STUDENT ACADEMIC SUMMARY\n");
        for (CourseStudent student : roster) {
            StudentAcademicSummary row = services.reporting().studentSummary(course.id(), student.studentId());
            overview.append(row.rollNumber()).append(" | ").append(row.studentName())
                    .append(" | Attendance: ").append(format(row.attendancePercentage()))
                    .append(" | CE: ").append(format(row.ceMark())).append("\n");

            CheckBox present = new CheckBox(student.toString());
            present.setSelected(true);
            attendanceChecks.put(student.studentId(), present);
            attendanceRows.getChildren().add(present);
        }
        summary.setText(overview.toString());
        components.getItems().setAll(services.evaluation().components(course.id()));
        markStudent.getItems().setAll(roster);
        weightStatus.setText("CE status: " + course.ceStatus() + " | Total weight: "
                + format(services.evaluation().totalWeight(course.id())) + "%");
        refreshResources(course);
    }

    private void refreshResources(Course course) {
        resources.getItems().setAll(services.resources().resources(course.id(), teacher.id()));
    }

    private void loadMark() {
        AssessmentComponent component = components.getSelectionModel().getSelectedItem();
        CourseStudent student = markStudent.getValue();
        if (component == null || student == null) {
            markField.clear();
            return;
        }
        Double mark = services.evaluation().mark(component.id(), student.studentId());
        markField.setText(mark == null ? "" : format(mark));
    }

    private Course selectedCourse() {
        return require(courseList.getSelectionModel().getSelectedItem(), "Select a course.");
    }

    private Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private VBox card(String heading, javafx.scene.Node... nodes) {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        box.setMaxWidth(820);
        box.getChildren().add(UiSupport.sectionTitle(heading));
        box.getChildren().addAll(nodes);
        return box;
    }

    private ScrollPane scroll(VBox content) {
        BorderPane wrapper = new BorderPane(content);
        wrapper.setPadding(new Insets(14));
        BorderPane.setAlignment(content, Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        return grid;
    }

    private static void addRow(GridPane grid, int row, String label, javafx.scene.control.Control control) {
        control.setMaxWidth(Double.MAX_VALUE);
        grid.add(new Label(label), 0, row);
        grid.add(control, 1, row);
        GridPane.setHgrow(control, Priority.ALWAYS);
    }

    private static TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        return field;
    }

    private <T> T require(T value, String message) {
        if (value == null) {
            throw new ValidationException(message);
        }
        return value;
    }

    private String format(Double value) {
        return value == null ? "—" : String.format("%.2f", value);
    }
}
