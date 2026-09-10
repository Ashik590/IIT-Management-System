package edu.du.iit.cms.ui;

import edu.du.iit.cms.AppServices;
import edu.du.iit.cms.domain.Role;
import edu.du.iit.cms.domain.UserSearchResult;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public final class PeopleDirectoryView extends BorderPane {
    private final AppServices services;
    private final Role role;
    private final boolean administrator;
    private final TextField search = new TextField();
    private final FlowPane results = new FlowPane(14, 14);

    public PeopleDirectoryView(AppServices services, Role role, boolean administrator, Node creationSection) {
        if (role != Role.STUDENT && role != Role.TEACHER) {
            throw new IllegalArgumentException("A people directory must be for Students or Teachers.");
        }
        this.services = services;
        this.role = role;
        this.administrator = administrator;

        String label = role == Role.STUDENT ? "Student" : "Teacher";
        search.setPromptText(role == Role.STUDENT
                ? "Search by name, username, roll number, or blood group"
                : "Search by name, username, or employee ID");
        Button searchButton = new Button("Search " + label + "s");
        searchButton.setOnAction(event -> performSearch());
        search.setOnAction(event -> performSearch());
        HBox searchRow = new HBox(8, search, searchButton);
        HBox.setHgrow(search, Priority.ALWAYS);

        VBox top = new VBox(14);
        if (creationSection != null) {
            top.getChildren().add(creationSection);
        }
        VBox searchCard = new VBox(10, UiSupport.sectionTitle(label + " directory"), searchRow,
                new Label("Leave the search field empty to show all " + label.toLowerCase() + "s."));
        searchCard.getStyleClass().add("card");
        top.getChildren().add(searchCard);
        setTop(top);

        results.setPadding(new Insets(16, 0, 16, 0));
        results.setPrefWrapLength(900);
        ScrollPane resultScroll = new ScrollPane(results);
        resultScroll.setFitToWidth(true);
        resultScroll.setPannable(true);
        setCenter(resultScroll);
        setPadding(new Insets(18));
    }

    private void performSearch() {
        List<UserSearchResult> matches = role == Role.STUDENT
                ? services.users().searchStudents(search.getText())
                : services.users().searchTeachers(search.getText());
        results.getChildren().clear();
        if (matches.isEmpty()) {
            results.getChildren().add(new Label("No matching "
                    + (role == Role.STUDENT ? "Students" : "Teachers") + " found."));
            return;
        }
        matches.forEach(result -> results.getChildren().add(personCard(result)));
    }

    private VBox personCard(UserSearchResult person) {
        String identifierLabel = role == Role.STUDENT ? "Roll number" : "Employee ID";
        String detailLabel = role == Role.STUDENT ? "Session and blood group" : "Designation";
        VBox card = new VBox(7,
                UiSupport.sectionTitle(person.fullName()),
                new Label("Username: " + person.username()),
                new Label("Email: " + person.email()),
                new Label(identifierLabel + ": " + person.identifier()),
                new Label(detailLabel + ": " + person.details()),
                new Label("Account status: " + (person.active() ? "Active" : "Inactive"))
        );
        card.getStyleClass().add("card");
        card.setPrefWidth(360);
        if (administrator) {
            Button toggle = new Button(person.active() ? "Deactivate" : "Activate");
            toggle.getStyleClass().add(person.active() ? "danger" : "secondary");
            toggle.setOnAction(event -> UiSupport.runAction("Account status updated.", () -> {
                services.users().setActive(person.id(), !person.active());
                performSearch();
            }));
            card.getChildren().add(toggle);
        }
        return card;
    }
}
