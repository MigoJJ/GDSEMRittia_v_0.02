// IttiaApp.java
package com.emr.gds;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.UnaryOperator;

public class IttiaApp extends Application {

    private final List<TextArea> areas = new ArrayList<>(10);

    // Titles for the center text areas
    public static final String[] TEXT_AREA_TITLES = {
            "CC>", "PI>", "ROS>", "PMH>", "S>",
            "O>", "Physical Exam>", "A>", "P>", "Comment>"
    };

    // Fonts (not strictly used here, but kept for future style work)
    private static final String BODY_FONT_FALLBACK =
            "Consolas, 'Nanum Gothic Coding', 'D2Coding', 'Noto Sans Mono', monospace";

    private ListProblemAction problemAction;
    private ListButtonAction buttonAction;

    private Connection dbConn;
    private final Map<String, String> abbrevMap = new HashMap<>();

    @Override
    public void start(Stage stage) {
        stage.setTitle("GDSEMR ITTIA – EMR Prototype (JavaFX)");

        // Initialize SQLite database for abbreviations
        initAbbrevDatabase();

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

    private void initAbbrevDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            dbConn = DriverManager.getConnection("jdbc:sqlite:abbreviations.db");
            Statement stmt = dbConn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS abbreviations (short TEXT PRIMARY KEY, full TEXT)");

            // Insert examples if not exist
            stmt.execute("INSERT OR IGNORE INTO abbreviations (short, full) VALUES ('c', 'hypercholesterolemia')");
            stmt.execute("INSERT OR IGNORE INTO abbreviations (short, full) VALUES ('to', 'hypothyroidism')");

            // Load into map
            ResultSet rs = stmt.executeQuery("SELECT * FROM abbreviations");
            while (rs.next()) {
                abbrevMap.put(rs.getString("short"), rs.getString("full"));
            }
            rs.close();
            stmt.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            // You may want to show a user-facing alert here
        }
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
            ta.setPrefRowCount(8);
            ta.setPrefColumnCount(40);

            // Set prompt text from our titles array
            String title = (i < TEXT_AREA_TITLES.length) ? TEXT_AREA_TITLES[i] : "Area " + (i + 1);
            ta.setPromptText(title);

            // Update scratchpad on change
            final int idx = i;
            if (idx < TEXT_AREA_TITLES.length) {
                ta.textProperty().addListener((obs, oldVal, newVal) ->
                        problemAction.updateAndRedrawScratchpad(TEXT_AREA_TITLES[idx], newVal));
            }

            // Abbreviation expansion:
            // Do NOT consume the SPACE event unless an actual replacement occurs.
            ta.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.SPACE) {
                    int caret = ta.getCaretPosition();
                    String text = ta.getText(0, caret);
                    int lastSpace = text.lastIndexOf(' ');
                    int lastNewline = text.lastIndexOf('\n');
                    int start = Math.max(lastSpace, lastNewline) + 1;
                    String word = text.substring(start);
                    if (word.startsWith(":")) {
                        String key = word.substring(1);
                        String replacement = key.equals("cd")
                                ? LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                                : abbrevMap.get(key);
                        if (replacement != null) {
                            ta.deleteText(start, caret);
                            ta.insertText(start, replacement + " ");
                            event.consume(); // consume ONLY when replacing
                        }
                    }
                }
            });

            // Filter weird control chars but allow tab/newline
            ta.setTextFormatter(new TextFormatter<>(filterControlChars()));

            int r = i / cols;
            int c = i % cols;
            // IMPORTANT: add TextArea directly; TextArea scrolls itself.
            grid.add(ta, c, r);
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
        ta.insertText(caret, block); // cleaner and fires listeners correctly
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
                    if (title.endsWith(">")) title = title.substring(0, title.length() - 1);
                } else {
                    title = "Area " + (i + 1);
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
        return areas.isEmpty() ? null : areas.get(0);
    }

    private void installGlobalShortcuts(Scene scene) {
        // Insert default template
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN),
                () -> insertTemplateIntoFocusedArea(ListButtonAction.TemplateLibrary.HPI)
        );

        // Auto format current area
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                this::formatCurrentArea
        );

        // Copy all
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                this::copyAllToClipboard
        );

        // Focus area 1..9 (Ctrl+1..9) and 10 (Ctrl+0)
        for (int i = 1; i <= 9; i++) {
            final int idx = i - 1;
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.getKeyCode(String.valueOf(i)), KeyCombination.CONTROL_DOWN),
                    () -> focusArea(idx)
            );
        }
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN),
                () -> focusArea(9)
        );
    }

    private void focusArea(int idx) {
        if (idx >= 0 && idx < areas.size()) {
            areas.get(idx).requestFocus();
        }
    }

    private void showToast(String message) {
        // Simple modal alert; replace with non-blocking snackbar if preferred
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Info");
        a.showAndWait();
    }

    // ===== Helpers =====

    private static java.util.function.UnaryOperator<TextFormatter.Change> filterControlChars() {
        return change -> {
            String text = change.getText();
            if (text == null || text.isEmpty()) return change;
            // Block ASCII control chars except TAB (U+0009) and LF (U+000A).
            // JavaFX uses '\n' (LF) for newlines.
            String filtered = text.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]", "");
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
                // trim trailing spaces
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
            s = s.replaceAll("^(#+)([^#\\n])", "$1 $2");
            // Guarantee single blank line between sections
            s = s.replaceAll("\\n{3,}", "\n\n");
            return s.trim();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
