package com.emr.gds;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Handles the UI and logic for managing the abbreviations database.
 * This class provides a modal dialog for adding, editing, and deleting
 * abbreviations, and for viewing the full list.
 */
public class dbControl {

    private final Connection dbConn;
    private final Map<String, String> abbrevMap;
    private final Stage ownerStage;
    private final IttiaApp parentApp; // Reference to the main app for UI updates

    public dbControl(Connection dbConn, Map<String, String> abbrevMap, Stage ownerStage, IttiaApp parentApp) {
        this.dbConn = dbConn;
        this.abbrevMap = abbrevMap;
        this.ownerStage = ownerStage;
        this.parentApp = parentApp;
    }

    /**
     * Shows a modal dialog for database management.
     */
    public void showDbManagerDialog() {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(ownerStage);
        stage.setTitle("Abbreviations Database Manager");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        // UI elements for CRUD
        TextField shortField = new TextField();
        shortField.setPromptText("Short");
        TextField fullField = new TextField();
        fullField.setPromptText("Full");

        Button findButton = new Button("Find");
        Button addButton = new Button("Add");
        Button editButton = new Button("Edit");
        Button deleteButton = new Button("Delete");
        Button refreshButton = new Button("Refresh List");

        // List View for displaying current abbreviations
        ListView<String> abbrevListView = new ListView<>();
        updateListView(abbrevListView);

        // Populate fields when an item is selected
        abbrevListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String[] parts = newVal.split(" -> ", 2);
                if (parts.length == 2) {
                    shortField.setText(parts[0]);
                    fullField.setText(parts[1]);
                }
            }
        });

        // Add actions to buttons
        addButton.setOnAction(e -> {
            String shortText = shortField.getText().trim();
            String fullText = fullField.getText().trim();
            add(shortText, fullText);
            updateListView(abbrevListView);
            clearFields(shortField, fullField);
        });

        editButton.setOnAction(e -> {
            String shortText = shortField.getText().trim();
            String fullText = fullField.getText().trim();
            edit(shortText, fullText);
            updateListView(abbrevListView);
        });

        deleteButton.setOnAction(e -> {
            String shortText = shortField.getText().trim();
            if (!shortText.isEmpty()) {
                delete(shortText);
                updateListView(abbrevListView);
                clearFields(shortField, fullField);
            }
        });

        findButton.setOnAction(e -> {
            String shortText = shortField.getText().trim();
            find(shortText);
        });

        refreshButton.setOnAction(e -> {
            updateListView(abbrevListView);
            clearFields(shortField, fullField);
        });

        // Layout the UI
        HBox inputFields = new HBox(10, shortField, fullField);
        HBox buttons = new HBox(10, addButton, editButton, deleteButton, findButton);

        grid.add(new Label("Abbreviations List:"), 0, 0);
        grid.add(abbrevListView, 0, 1, 2, 1);
        grid.add(inputFields, 0, 2, 2, 1);
        grid.add(buttons, 0, 3);
        grid.add(refreshButton, 1, 3);

        // CHANGE THIS LINE to increase the window size
        Scene scene = new Scene(grid, 700, 500); // Formerly 500, 400

        stage.setScene(scene);
        stage.show();
    }

    /**
     * Adds a new abbreviation to the database and map.
     */
    private void add(String shortText, String fullText) {
        if (shortText.isEmpty() || fullText.isEmpty()) {
            showAlert("Error", "Both fields must be filled.", Alert.AlertType.ERROR);
            return;
        }

        String sql = "INSERT OR IGNORE INTO abbreviations (short, full) VALUES (?, ?)";
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, shortText);
            pstmt.setString(2, fullText);
            pstmt.executeUpdate();
            // Also update the in-memory map
            abbrevMap.put(shortText, fullText);
            showAlert("Success", "Abbreviation added.", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to add abbreviation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Edits an existing abbreviation in the database and map.
     */
    private void edit(String shortText, String fullText) {
        if (shortText.isEmpty() || fullText.isEmpty()) {
            showAlert("Error", "Both fields must be filled.", Alert.AlertType.ERROR);
            return;
        }

        String sql = "UPDATE abbreviations SET full = ? WHERE short = ?";
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, fullText);
            pstmt.setString(2, shortText);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                abbrevMap.put(shortText, fullText);
                showAlert("Success", "Abbreviation updated.", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Warning", "Abbreviation not found.", Alert.AlertType.WARNING);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to edit abbreviation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Deletes an abbreviation from the database and map.
     */
    private void delete(String shortText) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Delete Abbreviation");
        confirmAlert.setContentText("Are you sure you want to delete '" + shortText + "'?");
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM abbreviations WHERE short = ?";
            try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
                pstmt.setString(1, shortText);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    abbrevMap.remove(shortText);
                    showAlert("Success", "Abbreviation deleted.", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Warning", "Abbreviation not found.", Alert.AlertType.WARNING);
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to delete abbreviation: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Finds an abbreviation and shows an alert.
     */
    private void find(String shortText) {
        String fullText = abbrevMap.get(shortText);
        if (fullText != null) {
            showAlert("Found", "Found: " + shortText + " -> " + fullText, Alert.AlertType.INFORMATION);
        } else {
            showAlert("Not Found", "Abbreviation '" + shortText + "' was not found.", Alert.AlertType.INFORMATION);
        }
    }
    
    /**
     * Populates the ListView with all abbreviations.
     */
    private void updateListView(ListView<String> listView) {
        // Use a stream to sort the map entries by key and collect them into an ObservableList
        ObservableList<String> items = abbrevMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + " -> " + entry.getValue())
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        listView.setItems(items);
    }

    /**
     * Clears the input fields.
     */
    private void clearFields(TextField shortField, TextField fullField) {
        shortField.clear();
        fullField.clear();
        shortField.requestFocus();
    }

    /**
     * Helper method to show a user-facing alert.
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
