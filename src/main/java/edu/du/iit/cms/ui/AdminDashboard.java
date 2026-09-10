package edu.du.iit.cms.ui;

import edu.du.iit.cms.AppServices;
import edu.du.iit.cms.domain.Course;
import edu.du.iit.cms.domain.CourseStudent;
import edu.du.iit.cms.domain.CourseType;
import edu.du.iit.cms.domain.Role;
import edu.du.iit.cms.domain.Student;
import edu.du.iit.cms.domain.StudentAcademicSummary;
import edu.du.iit.cms.domain.Teacher;
import edu.du.iit.cms.domain.User;
import edu.du.iit.cms.domain.UserSearchResult;
import edu.du.iit.cms.service.ValidationException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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

import java.util.List;

public final class AdminDashboard extends BorderPane {
    private final AppServices services;
    private final ObservableList<Course> courses = FXCollections.observableArrayList();
    private final ObservableList<Student> students = FXCollections.observableArrayList();
    private final ObservableList<Teacher> teachers = FXCollections.observableArrayList();

    public AdminDashboard(AppServices services, User user, Runnable logout) {
        this.services = services;
        setTop(UiSupport.header("Administrator Dashboard", user.fullName(), logout));

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                tab("Users", createUsersPane()),
                tab("Courses and Allocation", createCoursesPane()),
                tab("Final Results", createResultsPane())
        );
        setCenter(tabs);
        refreshSharedData();
    }

    private ScrollPane createUsersPane() {
        ComboBox<Role> role = new ComboBox<>(FXCollections.observableArrayList(Role.STUDENT, Role.TEACHER));
        role.setValue(Role.STUDENT);
        TextField username = field("Unique username");
        TextField password = field("Initial password, minimum 6 characters");
        TextField fullName = field("Full name");
        TextField email = field("Email");
        TextField identifier = field("Roll number");
        TextField detailOne = field("Academic session");
        TextField detailTwo = field("Blood group");

        role.valueProperty().addListener((observable, oldValue, value) -> {
            if (value == Role.STUDENT) {
                identifier.setPromptText("Roll number");
                detailOne.setPromptText("Academic session");
                detailTwo.setPromptText("Blood group");
                detailTwo.setDisable(false);
            } else {
                identifier.setPromptText("Employee ID");
                detailOne.setPromptText("Designation");
                detailTwo.clear();
                detailTwo.setPromptText("Not used for Teachers");
                detailTwo.setDisable(true);
            }
        });

        GridPane form = grid();
        addRow(form, 0, "Role", role);
        addRow(form, 1, "Username", username);
        addRow(form, 2, "Password", password);
        addRow(form, 3, "Full name", fullName);
        addRow(form, 4, "Email", email);
        addRow(form, 5, "Identifier", identifier);
        addRow(form, 6, "Profile detail", detailOne);
        addRow(form, 7, "Blood group", detailTwo);

        Button create = new Button("Create account");
        create.setOnAction(event -> UiSupport.runAction("Account created.", () -> {
            if (role.getValue() == Role.STUDENT) {
                services.users().createStudent(username.getText(), password.getText(), fullName.getText(),
                        email.getText(), identifier.getText(), detailOne.getText(), detailTwo.getText());
            } else {
                services.users().createTeacher(username.getText(), password.getText(), fullName.getText(),
                        email.getText(), identifier.getText(), detailOne.getText());
            }
            username.clear();
            password.clear();
            fullName.clear();
            email.clear();
            identifier.clear();
            detailOne.clear();
            detailTwo.clear();
            refreshSharedData();
        }));

        TextField search = field("Name, username, roll, blood group, or employee ID");
        ObservableList<UserSearchResult> results = FXCollections.observableArrayList();
        ListView<UserSearchResult> resultList = new ListView<>(results);
        resultList.setPrefHeight(260);
        Button searchButton = new Button("Search");
        Runnable performSearch = () -> results.setAll(services.users().search(search.getText()));
        searchButton.setOnAction(event -> performSearch.run());
        search.setOnAction(event -> performSearch.run());

        Button toggle = new Button("Activate / deactivate selected");
        toggle.getStyleClass().add("secondary");
        toggle.setOnAction(event -> UiSupport.runAction("Account status updated.", () -> {
            UserSearchResult selected = require(resultList.getSelectionModel().getSelectedItem(), "Select a user.");
            services.users().setActive(selected.id(), !selected.active());
            performSearch.run();
            refreshSharedData();
        }));

        VBox content = card("Create Student or Teacher", form, create,
                UiSupport.sectionTitle("Search and account status"), new HBox(8, search, searchButton),
                resultList, toggle);
        HBox.setHgrow(search, Priority.ALWAYS);
        return scroll(content);
    }

    private ScrollPane createCoursesPane() {
        TextField code = field("SE-2215");
        TextField title = field("Course title");
        ComboBox<CourseType> type = new ComboBox<>(FXCollections.observableArrayList(CourseType.values()));
        type.setValue(CourseType.THEORY);
        TextField credit = field("3.0");
        TextField session = field("2025-26");
        TextField semester = field("5th");
        GridPane form = grid();
        addRow(form, 0, "Course code", code);
        addRow(form, 1, "Title", title);
        addRow(form, 2, "Type", type);
        addRow(form, 3, "Credit", credit);
        addRow(form, 4, "Session", session);
        addRow(form, 5, "Semester", semester);

        ListView<Course> courseList = new ListView<>(courses);
        courseList.setPrefHeight(230);
        ComboBox<Teacher> teacherChoice = new ComboBox<>(teachers);
        ComboBox<Student> studentChoice = new ComboBox<>(students);
        teacherChoice.setMaxWidth(Double.MAX_VALUE);
        studentChoice.setMaxWidth(Double.MAX_VALUE);
        ListView<Teacher> assignedTeachers = new ListView<>();
        ListView<CourseStudent> roster = new ListView<>();
        assignedTeachers.setPrefHeight(110);
        roster.setPrefHeight(150);

        Runnable refreshSelection = () -> {
            Course selected = courseList.getSelectionModel().getSelectedItem();
            assignedTeachers.getItems().clear();
            roster.getItems().clear();
            if (selected != null) {
                code.setText(selected.courseCode());
                title.setText(selected.title());
                type.setValue(selected.courseType());
                credit.setText(format(selected.credit()));
                session.setText(selected.academicSession());
                semester.setText(selected.semester());
                assignedTeachers.getItems().setAll(services.courses().teachers(selected.id()));
                roster.getItems().setAll(services.courses().students(selected.id()));
            }
        };
        courseList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, value) -> refreshSelection.run());

        Button create = new Button("Create Draft course");
        create.setOnAction(event -> UiSupport.runAction("Course created.", () -> {
            services.courses().createCourse(code.getText(), title.getText(), type.getValue(),
                    UiSupport.number(credit.getText(), "Credit"), session.getText(), semester.getText());
            code.clear();
            title.clear();
            refreshSharedData();
        }));

        Button update = new Button("Update selected Draft course");
        update.getStyleClass().add("secondary");
        update.setOnAction(event -> UiSupport.runAction("Draft course updated.", () -> {
            Course selected = require(courseList.getSelectionModel().getSelectedItem(), "Select a course.");
            services.courses().updateCourse(selected.id(), code.getText(), title.getText(), type.getValue(),
                    UiSupport.number(credit.getText(), "Credit"), session.getText(), semester.getText());
            long selectedId = selected.id();
            refreshSharedData();
            courses.stream().filter(course -> course.id() == selectedId).findFirst()
                    .ifPresent(courseList.getSelectionModel()::select);
        }));

        Button delete = new Button("Delete selected Draft course");
        delete.getStyleClass().add("danger");
        delete.setOnAction(event -> {
            Course selected = courseList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                UiSupport.showError("Select a course.");
            } else if (UiSupport.confirm("Delete Draft course " + selected.courseCode()
                    + "? Its Draft allocations will also be removed.")) {
                UiSupport.runAction("Draft course deleted.", () -> {
                    services.courses().deleteCourse(selected.id());
                    refreshSharedData();
                });
            }
        });

        Button assign = new Button("Assign Teacher");
        assign.setOnAction(event -> UiSupport.runAction("Teacher assigned.", () -> {
            Course selected = require(courseList.getSelectionModel().getSelectedItem(), "Select a course.");
            Teacher teacher = require(teacherChoice.getValue(), "Select a Teacher.");
            services.courses().assignTeacher(selected.id(), teacher.id());
            refreshSharedData();
            refreshSelection.run();
        }));

        Button removeTeacher = new Button("Remove selected Teacher");
        removeTeacher.getStyleClass().add("secondary");
        removeTeacher.setOnAction(event -> UiSupport.runAction("Teacher allocation removed.", () -> {
            Course selected = require(courseList.getSelectionModel().getSelectedItem(), "Select a course.");
            Teacher assigned = require(assignedTeachers.getSelectionModel().getSelectedItem(),
                    "Select an assigned Teacher.");
            services.courses().removeTeacher(selected.id(), assigned.id());
            refreshSelection.run();
        }));

        Button enroll = new Button("Enroll Student");
        enroll.setOnAction(event -> UiSupport.runAction("Student enrolled.", () -> {
            Course selected = require(courseList.getSelectionModel().getSelectedItem(), "Select a course.");
            Student student = require(studentChoice.getValue(), "Select a Student.");
            services.courses().enrollStudent(selected.id(), student.id());
            refreshSharedData();
            refreshSelection.run();
        }));

        Button removeStudent = new Button("Remove selected Student");
        removeStudent.getStyleClass().add("secondary");
        removeStudent.setOnAction(event -> UiSupport.runAction("Student enrollment removed.", () -> {
            Course selected = require(courseList.getSelectionModel().getSelectedItem(), "Select a course.");
            CourseStudent enrolled = require(roster.getSelectionModel().getSelectedItem(),
                    "Select an enrolled Student.");
            services.courses().removeStudent(selected.id(), enrolled.studentId());
            refreshSelection.run();
        }));

        Button activate = new Button("Activate selected course");
        activate.setOnAction(event -> UiSupport.runAction("Course activated.", () -> {
            Course selected = require(courseList.getSelectionModel().getSelectedItem(), "Select a course.");
            services.courses().activateCourse(selected.id());
            refreshSharedData();
        }));

        VBox content = card("Create or edit course", form, new HBox(8, create, update, delete),
                UiSupport.sectionTitle("Configure Draft course"), courseList,
                new Label("Assigned Teachers"), assignedTeachers,
                new HBox(8, teacherChoice, assign), removeTeacher,
                new Label("Enrolled Students"), roster,
                new HBox(8, studentChoice, enroll), removeStudent, activate);
        HBox.setHgrow(teacherChoice, Priority.ALWAYS);
        HBox.setHgrow(studentChoice, Priority.ALWAYS);
        return scroll(content);
    }

    private ScrollPane createResultsPane() {
        ComboBox<Course> courseChoice = new ComboBox<>(courses);
        courseChoice.setMaxWidth(Double.MAX_VALUE);
        ComboBox<CourseStudent> studentChoice = new ComboBox<>();
        studentChoice.setMaxWidth(Double.MAX_VALUE);
        Label currentCe = new Label("CE: —");
        TextField finalMark = field("Select a course");
        TextArea report = new TextArea();
        report.setEditable(false);
        report.setPrefRowCount(16);

        Runnable refreshReport = () -> {
            Course course = courseChoice.getValue();
            studentChoice.getItems().clear();
            report.clear();
            if (course == null) {
                finalMark.setPromptText("Select a course");
                return;
            }
            finalMark.setPromptText("0 to " + course.courseType().finalExamMarks());
            List<CourseStudent> roster = services.courses().students(course.id());
            studentChoice.getItems().setAll(roster);
            StringBuilder text = new StringBuilder();
            text.append(course).append("\n\n");
            for (StudentAcademicSummary row : services.reporting().courseResultSheet(course.id())) {
                text.append(row.rollNumber()).append(" | ").append(row.studentName())
                        .append(" | Attendance: ").append(format(row.attendancePercentage()))
                        .append(" | CE: ").append(format(row.ceMark()))
                        .append(" | Final: ").append(format(row.finalExamMark()))
                        .append(" | Total: ").append(format(row.totalMark()))
                        .append(" | ").append(row.enrollmentStatus()).append('\n');
            }
            report.setText(text.toString());
        };
        courseChoice.valueProperty().addListener((observable, oldValue, value) -> refreshReport.run());
        studentChoice.valueProperty().addListener((observable, oldValue, value) -> {
            if (value == null || courseChoice.getValue() == null) {
                currentCe.setText("CE: —");
                finalMark.clear();
            } else {
                double ce = services.evaluation().calculateCe(courseChoice.getValue().id(), value.studentId());
                currentCe.setText("CE: " + format(ce) + " / "
                        + courseChoice.getValue().courseType().ceMarks());
                finalMark.setText(value.finalExamMark() == null ? "" : format(value.finalExamMark()));
            }
        });

        Button saveMark = new Button("Save final-exam mark");
        saveMark.setOnAction(event -> UiSupport.runAction("Final-exam mark saved.", () -> {
            Course course = require(courseChoice.getValue(), "Select a course.");
            CourseStudent student = require(studentChoice.getValue(), "Select a Student.");
            services.courses().saveFinalExamMark(course.id(), student.studentId(),
                    UiSupport.number(finalMark.getText(), "Final-exam mark"));
            refreshSharedData();
            refreshReport.run();
        }));

        Button finish = new Button("Validate and finish course");
        finish.getStyleClass().add("danger");
        finish.setOnAction(event -> {
            Course course = courseChoice.getValue();
            if (course == null) {
                UiSupport.showError("Select a course.");
            } else if (UiSupport.confirm("Finish " + course.courseCode() + "? All academic records will become read-only.")) {
                UiSupport.runAction("Course finished and final results stored.", () -> {
                    services.completion().finish(course.id());
                    refreshSharedData();
                    courseChoice.setValue(services.courses().get(course.id()));
                    refreshReport.run();
                });
            }
        });

        VBox content = card("Final examination and course completion", new Label("Course"), courseChoice,
                new Label("Student"), studentChoice, currentCe,
                new HBox(8, finalMark, saveMark), finish,
                UiSupport.sectionTitle("Course result sheet"), report);
        HBox.setHgrow(finalMark, Priority.ALWAYS);
        return scroll(content);
    }

    private void refreshSharedData() {
        courses.setAll(services.courses().allCourses());
        students.setAll(services.users().activeStudents());
        teachers.setAll(services.users().activeTeachers());
    }

    private Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private VBox card(String heading, javafx.scene.Node... nodes) {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        box.setMaxWidth(900);
        box.getChildren().add(UiSupport.sectionTitle(heading));
        box.getChildren().addAll(nodes);
        return box;
    }

    private ScrollPane scroll(VBox content) {
        BorderPane wrapper = new BorderPane(content);
        wrapper.setPadding(new Insets(18));
        BorderPane.setAlignment(content, Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        return grid;
    }

    private void addRow(GridPane grid, int row, String label, javafx.scene.control.Control control) {
        control.setMaxWidth(Double.MAX_VALUE);
        grid.add(new Label(label), 0, row);
        grid.add(control, 1, row);
        GridPane.setHgrow(control, Priority.ALWAYS);
    }

    private TextField field(String prompt) {
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
