// ListProblemAction.java
package com.emr.gds;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.*;

public class ListProblemAction {

    private final IttiaApp app;

    // Problem list (left)
    private final ObservableList<String> problems = FXCollections.observableArrayList(
            "Hypercholesterolemia [F/U]",
            "Prediabetes (FBS 108 mg/dL)",
            "Thyroid nodule (small)"
    );

    private ListView<String> problemList;

    private TextArea scratchpadArea; // Promoted to a field
    private final LinkedHashMap<String, String> scratchpadEntries = new LinkedHashMap<>();

    public ListProblemAction(IttiaApp app) {
        this.app = app;
    }

    public VBox buildProblemPane() {
        // --- Problem List Section ---
        problemList = new ListView<>(problems);
        problemList.setPrefWidth(320);
        problemList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String sel = problemList.getSelectionModel().getSelectedItem();
                if (sel != null) app.insertLineIntoFocusedArea("- " + sel);
            }
        });

        TextField input = new TextField();
        input.setPromptText("Add problem and press Enter");
        input.setOnAction(e -> {
            String text = IttiaApp.normalizeLine(input.getText());
            if (!text.isBlank()) {
                problems.add(text);
                input.clear();
            }
        });

        Button remove = new Button("Remove Selected");
        remove.setOnAction(e -> {
            int idx = problemList.getSelectionModel().getSelectedIndex();
            if (idx >= 0) problems.remove(idx);
        });

        HBox problemControls = new HBox(8, input, remove);
        HBox.setHgrow(input, Priority.ALWAYS);

        // --- Scratchpad Section ---
        this.scratchpadArea = new TextArea();
        scratchpadArea.setPromptText("Scratchpad... (auto-updated from center areas)");
        scratchpadArea.setWrapText(true);
        scratchpadArea.setPrefRowCount(8);
        scratchpadArea.setEditable(true); // Keep it editable for manual notes

        // --- Assemble the VBox with INVERTED order ---
        VBox box = new VBox(8,
                new Label("Scratchpad"),
                scratchpadArea,
                new Separator(Orientation.HORIZONTAL),
                new Label("Problem List"),
                problemList,
                problemControls
        );

        // Make both the list and the scratchpad grow to fill vertical space
        VBox.setVgrow(problemList, Priority.ALWAYS);
        VBox.setVgrow(scratchpadArea, Priority.ALWAYS);
        box.setPadding(new Insets(0, 10, 0, 0));
        return box;
    }

    public void updateAndRedrawScratchpad(String title, String newText) {
        String trimmedText = newText.trim();

        if (trimmedText.isEmpty()) {
            // If the text area is cleared, remove its entry from the scratchpad
            scratchpadEntries.remove(title);
        } else {
            // Replace newlines with a visual separator to keep each entry on one line in the scratchpad
            String singleLineText = trimmedText.replaceAll("\\s*\\R\\s*", " \n\t ");
            scratchpadEntries.put(title, singleLineText);
        }

        redrawScratchpad();
    }

    public void redrawScratchpad() {
        if (scratchpadArea == null) return; // Guard against early calls before UI is built

        List<String> orderedTitles = Arrays.asList(IttiaApp.TEXT_AREA_TITLES);
        StringJoiner sj = new StringJoiner("\n");
        for (String title : orderedTitles) {
            String value = scratchpadEntries.get(title);
            if (value != null && !value.isEmpty()) {
                sj.add(title + " " + value);
            }
        }

        // To avoid losing manual edits, we only overwrite if the content has changed
        // This is a simple check; more complex logic could be used if needed.
        if (!scratchpadArea.getText().equals(sj.toString())) {
            scratchpadArea.setText(sj.toString());
            scratchpadArea.positionCaret(scratchpadArea.getLength());
            scratchpadArea.setScrollTop(Double.MAX_VALUE); // Scroll to bottom
        }
    }

    public ObservableList<String> getProblems() {
        return problems;
    }
}