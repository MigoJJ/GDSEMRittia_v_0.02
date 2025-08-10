// IttiaApp.java
package com.emr.gds;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;

public class App extends Application {

    private final List<TextArea> areas = new ArrayList<>(10);

    // Titles for the center text areas
    public static final String[] TEXT_AREA_TITLES = {
            "CC>", "PI>", "ROS>", "PMH>", "S>",
            "O>", "Physical Exam>", "A>", "P>", "Comment>"
    };

    // Fonts
    private static final String BODY_FONT_FALLBACK = "Consolas, 'Nanum Gothic Coding', 'D2Coding', 'Noto Sans Mono', monospace";

    private ListProblemAction problemAction;
    private ListButtonAction buttonAction;

    @Override
    public void start(Stage stage) {
        stage.setTitle("GDSEMR ITTIA – EMR Prototype (JavaFX)");

        problemAction = new ListProblemAction(this);
        buttonAction = new ListButtonAction(this);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // ==== Top bar (commands) ====
        root.setTop(buttonAction.buildTopBar());

        // ==== Left (Problem List & Scratchpad) ====
        root.setLeft(problemAction.buildProblemPane());

        // ==== Center (10 template areas) ====
        root.setCenter(buildCenterAreas());

        // ==== Bottom (quick snippets) ====
        root.setBottom(buttonAction.buildBottomBar());

        Scene scene = new Scene(root, 1400, 840);
        stage.setScene(scene);
        stage.show();

        // Focus first area shortly after show
        Platform.runLater(() -> areas.get(0).requestFocus());

        // Global accelerators
        installGlobalShortcuts(scene);
    }

    private GridPane buildCenterAreas() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        int rows = 5, cols = 2;
        for (int i = 0; i < rows * cols; i++) {
            TextArea ta = new TextArea();
            ta.setWrapText(true);
            ta.setFont(Font.font("Monospaced", 13));

            // Set prompt text from our titles array
            String title = (i < TEXT_AREA_TITLES.length) ? TEXT_AREA_TITLES[i] : "Area " + (i + 1);
            ta.setPromptText(title);

            // Add listener to update the scratchpad
            final int idx = i;
            if (idx < TEXT_AREA_TITLES.length) {
                ta.textProperty().addListener((obs, oldVal, newVal) -> {
                    problemAction.updateAndRedrawScratchpad(TEXT_AREA_TITLES[idx], newVal);
                });
            }

            // Optional: restrict control chars except tab/newline
            ta.setTextFormatter(new TextFormatter<>(filterControlChars()));

            ScrollPane sp = new ScrollPane(ta);
            sp.setFitToWidth(true);
            sp.setFitToHeight(true);
            sp.setPrefViewportHeight(150);

            int r = i / cols;
            int c = i % cols;
            grid.add(sp, c, r);
            areas.add(ta);
        }
        return grid;
    }

    // ===== Actions =====

    public void insertTemplateIntoFocusedArea(ListButtonAction.TemplateLibrary t) {
        TextArea ta = getFocusedArea();
        if (ta == null) return;
        insertBlock(ta, t.body());
    }

    public void insertLineIntoFocusedArea(String line) {
        TextArea ta = getFocusedArea();
        if (ta == null) return;
        String insert = line.endsWith("\n") ? line : line + "\n";
        insertBlock(ta, insert);
    }

    public void insertBlockIntoFocusedArea(String block) {
        TextArea ta = getFocusedArea();
        if (ta == null) return;
        insertBlock(ta, block);
    }

    private void insertBlock(TextArea ta, String block) {
        int caret = ta.getCaretPosition();
        ta.insertText(caret, block); // Using insertText is cleaner and fires listeners correctly
    }

    public void formatCurrentArea() {
        TextArea ta = getFocusedArea();
        if (ta == null) return;
        ta.setText(Formatter.autoFormat(ta.getText()));
    }

    public void copyAllToClipboard() {
        StringJoiner sj = new StringJoiner("\n\n");

        // Problems -> bullet list
        ObservableList<String> problems = problemAction.getProblems();
        if (!problems.isEmpty()) {
            StringBuilder pb = new StringBuilder();
            pb.append("# Problem List (as of ")
              .append(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
              .append(")\n");
            for (String p : problems) pb.append("- ").append(p).append("\n");
            sj.add(pb.toString().trim());
        }

        // Areas
        for (int i = 0; i < areas.size(); i++) {
            String txt = areas.get(i).getText().trim();
            if (!txt.isEmpty()) {
                String title;
                if (i < TEXT_AREA_TITLES.length) {
                    title = TEXT_AREA_TITLES[i];
                    // Clean up title for final output (e.g., "CC>" becomes "CC")
                    if (title.endsWith(">")) {
                        title = title.substring(0, title.length() - 1);
                    }
                } else {
                    title = "Area " + (i + 1); // Fallback
                }
                sj.add("# " + title + "\n" + txt);
            }
        }

        String result = Formatter.finalizeForEMR(sj.toString());

        ClipboardContent cc = new ClipboardContent();
        cc.putString(result);
        Clipboard.getSystemClipboard().setContent(cc);

        showToast("Copied all content to clipboard");
    }

    private TextArea getFocusedArea() {
        for (TextArea ta : areas) {
            if (ta.isFocused()) return ta;
        }
        // If none focused, pick Area 1
        return areas.isEmpty() ? null : areas.get(0);
    }

    private void installGlobalShortcuts(Scene scene) {
        // Insert default template
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN),
                () -> insertTemplateIntoFocusedArea(ListButtonAction.TemplateLibrary.HPI));

        // Auto format current area
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                this::formatCurrentArea);

        // Copy all
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                this::copyAllToClipboard);

        // Focus area 1..9 (Ctrl+1..9) and 10 (Ctrl+0)
        for (int i = 1; i <= 9; i++) {
            final int idx = i - 1;
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.getKeyCode(String.valueOf(i)), KeyCombination.CONTROL_DOWN),
                    () -> focusArea(idx));
        }
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN),
                () -> focusArea(9));
    }

    private void focusArea(int idx) {
        if (idx >= 0 && idx < areas.size()) {
            areas.get(idx).requestFocus();
        }
    }

    private void showToast(String message) {
        // Lightweight toast using Alert (simple & blocking). Replace with non-blocking snackbar as needed.
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Info");
        a.showAndWait();
    }

    // ===== Helpers =====

    private static UnaryOperator<TextFormatter.Change> filterControlChars() {
        return change -> {
            String text = change.getText();
            if (text == null) return change;
            // allow normal text; block weird control chars except tab/newline
            String filtered = text.replaceAll("[-\u0008\u000B\u000C\u000E-\u001F]", "");
            change.setText(filtered);
            return change;
        };
    }

    public static String normalizeLine(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    // ===== Formatting utilities =====
    public static class Formatter {
        /**
         * Normalize bullets, collapse blank lines, trim trailing spaces.
         */
        static String autoFormat(String raw) {
            if (raw == null || raw.isBlank()) return "";
            String[] lines = raw.replace("\r", "").split("\n", -1);
            StringBuilder out = new StringBuilder();
            boolean lastBlank = false;
            for (String line : lines) {
                String t = line.strip();
                // Normalize bullets to "- "
                t = t.replaceAll("^[•·→▶▷‣⦿∘*]+\\s*", "- ");
                t = t.replaceAll("^[-]{1,2}\\s*", "- ");
                // collapse internal spaces
                t = t.replaceAll("\\s+$", "");

                if (t.isEmpty()) {
                    if (!lastBlank) {
                        out.append("\n");
                        lastBlank = true;
                    }
                } else {
                    out.append(t).append("\n");
                    lastBlank = false;
                }
            }
            return out.toString().strip();
        }

        /**
         * Final pass for EMR export: ensure headers start with '# ' and
         * ensure a clean single blank line between sections.
         */
        static String finalizeForEMR(String raw) {
            String s = autoFormat(raw);
            // Ensure markdown-like headers start with '# '
            s = s.replaceAll("^(#+)([^#\n])", "$1 $2");
            // Guarantee single blank line between sections
            s = s.replaceAll("\n{3,}", "\n\n");
            return s.trim();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}