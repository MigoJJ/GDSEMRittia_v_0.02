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

/**
 * JavaFX EMR prototype tailored for readability and real-world actions.
 *
 * Layout
 *  - Top: command buttons + template menu + shortcuts
 *  - Left: Problem List (add/remove, double-click to send to focused area)
 *  - Center: 10 editable areas (5 x 2), each scrollable
 *  - Bottom: quick-snippet buttons (1..7)
 *
 * Actions
 *  - Insert Template (menu + Ctrl+I)
 *  - Auto Format current area (Ctrl+Shift+F)
 *  - Copy All (aggregated EMR note -> clipboard) (Ctrl+Shift+C)
 *  - Focus area N: Ctrl+1..Ctrl+0 (10)
 */
public class App extends Application {

    private final List<TextArea> areas = new ArrayList<>(10);

    // Problem list (left)
    private final ObservableList<String> problems = FXCollections.observableArrayList(
            "Hypercholesterolemia [F/U]",
            "Prediabetes (FBS 108 mg/dL)",
            "Thyroid nodule (small)"
    );

    private ListView<String> problemList;

    // Fonts
    private static final String BODY_FONT_FALLBACK = "Consolas, 'Nanum Gothic Coding', 'D2Coding', 'Noto Sans Mono', monospace";

    @Override
    public void start(Stage stage) {
        stage.setTitle("GDSEMR ITTIA – EMR Prototype (JavaFX)");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // ==== Top bar (commands) ====
        ToolBar topBar = buildTopBar();
        root.setTop(topBar);

        // ==== Left (Problem List) ====
        root.setLeft(buildProblemPane());

        // ==== Center (10 template areas) ====
        root.setCenter(buildCenterAreas());

        // ==== Bottom (quick snippets) ====
        root.setBottom(buildBottomBar());

        Scene scene = new Scene(root, 1400, 840);
        stage.setScene(scene);
        stage.show();

        // Focus first area shortly after show
        Platform.runLater(() -> areas.get(0).requestFocus());

        // Global accelerators
        installGlobalShortcuts(scene);
    }

    private ToolBar buildTopBar() {
        Button btnInsertTemplate = new Button("Insert Template (Ctrl+I)");
        btnInsertTemplate.setOnAction(e -> insertTemplateIntoFocusedArea(TemplateLibrary.HPI));

        Button btnFormat = new Button("Auto Format (Ctrl+Shift+F)");
        btnFormat.setOnAction(e -> formatCurrentArea());

        Button btnCopyAll = new Button("Copy All (Ctrl+Shift+C)");
        btnCopyAll.setOnAction(e -> copyAllToClipboard());

        // Templates menu
        MenuButton templatesMenu = new MenuButton("Templates");
        for (TemplateLibrary t : TemplateLibrary.values()) {
            MenuItem mi = new MenuItem(t.displayName());
            mi.setOnAction(e -> insertTemplateIntoFocusedArea(t));
            templatesMenu.getItems().add(mi);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Focus area: Ctrl+1..Ctrl+0 | Double-click problem to insert");

        ToolBar tb = new ToolBar(
                btnInsertTemplate,
                templatesMenu,
                new Separator(),
                btnFormat,
                btnCopyAll,
                spacer,
                hint
        );
        return tb;
    }

    private VBox buildProblemPane() {
        problemList = new ListView<>(problems);
        problemList.setPrefWidth(320);
        problemList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String sel = problemList.getSelectionModel().getSelectedItem();
                if (sel != null) insertLineIntoFocusedArea("- " + sel);
            }
        });

        TextField input = new TextField();
        input.setPromptText("Add problem and press Enter");
        input.setOnAction(e -> {
            String text = normalizeLine(input.getText());
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

        VBox box = new VBox(8,
                new Label("Problem List"),
                problemList,
                new HBox(8, input, remove)
        );
        VBox.setVgrow(problemList, Priority.ALWAYS);
        box.setPadding(new Insets(0, 10, 0, 0));
        return box;
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
            ta.setPromptText("Area " + (i + 1));
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

    private ToolBar buildBottomBar() {
        // 7 quick snippet buttons
        Button b1 = quickSnippetButton("Vitals", TemplateLibrary.SNIPPET_VITALS.body());
        Button b2 = quickSnippetButton("Meds", TemplateLibrary.SNIPPET_MEDS.body());
        Button b3 = quickSnippetButton("Allergy", TemplateLibrary.SNIPPET_ALLERGY.body());
        Button b4 = quickSnippetButton("Assessment", TemplateLibrary.SNIPPET_ASSESS.body());
        Button b5 = quickSnippetButton("Plan", TemplateLibrary.SNIPPET_PLAN.body());
        Button b6 = quickSnippetButton("F/U", TemplateLibrary.SNIPPET_FOLLOWUP.body());
        Button b7 = quickSnippetButton("Signature", TemplateLibrary.SNIPPET_SIGNATURE.body());

        ToolBar tb = new ToolBar(b1, b2, b3, b4, b5, b6, b7);
        tb.setPadding(new Insets(8, 0, 0, 0));
        return tb;
    }

    private Button quickSnippetButton(String title, String snippet) {
        Button b = new Button(title);
        b.setOnAction(e -> insertBlockIntoFocusedArea(snippet));
        return b;
    }

    // ===== Actions =====

    private void insertTemplateIntoFocusedArea(TemplateLibrary t) {
        TextArea ta = getFocusedArea();
        if (ta == null) return;
        insertBlock(ta, t.body());
    }

    private void insertLineIntoFocusedArea(String line) {
        TextArea ta = getFocusedArea();
        if (ta == null) return;
        String insert = line.endsWith("\n") ? line : line + "\n";
        insertBlock(ta, insert);
    }

    private void insertBlockIntoFocusedArea(String block) {
        TextArea ta = getFocusedArea();
        if (ta == null) return;
        insertBlock(ta, block);
    }

    private void insertBlock(TextArea ta, String block) {
        int caret = ta.getCaretPosition();
        StringBuilder sb = new StringBuilder(ta.getText());
        sb.insert(caret, block);
        ta.setText(sb.toString());
        ta.positionCaret(caret + block.length());
    }

    private void formatCurrentArea() {
        TextArea ta = getFocusedArea();
        if (ta == null) return;
        ta.setText(Formatter.autoFormat(ta.getText()));
    }

    private void copyAllToClipboard() {
        StringJoiner sj = new StringJoiner("\n\n");

        // Problems -> bullet list
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
                sj.add("# Area " + (i + 1) + "\n" + txt);
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
                () -> insertTemplateIntoFocusedArea(TemplateLibrary.HPI));

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
            String filtered = text.replaceAll("[\u0000-\u0008\u000B\u000C\u000E-\u001F]", "");
            change.setText(filtered);
            return change;
        };
    }

    private static String normalizeLine(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    // ===== Template library =====

    private enum TemplateLibrary {
        HPI("HPI",
                "# HPI\n" +
                "- Onset: \n" +
                "- Location: \n" +
                "- Character: \n" +
                "- Aggravating/Relieving: \n" +
                "- Associated Sx: \n" +
                "- Context: \n" +
                "- Notes: \n"),
        A_P("Assessment & Plan",
                "# Assessment & Plan\n" +
                "- Dx: \n" +
                "- Severity: \n" +
                "- Plan: meds / labs / imaging / follow-up\n"),
        LETTER("Letter Template",
                "# Letter\n" +
                "Patient: \nDOB: \nDate: " + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + "\n\n" +
                "Findings:\n- \n\nPlan:\n- \n\nSignature:\nMigoJJ, MD\n"),
        LAB_SUMMARY("Lab Summary",
                "# Labs\n" +
                "- FBS:  mg/dL\n" +
                "- LDL:  mg/dL\n" +
                "- HbA1c:  %\n" +
                "- TSH:  uIU/mL\n"),
        PROBLEM_LIST("Problem List Header",
                "# Problem List\n- \n- \n- \n"),

        // Quick snippets (bottom bar)
        SNIPPET_VITALS("Vitals",
                "# Vitals\n- BP: / mmHg\n- HR: / min\n- Temp:  °C\n- RR: / min\n- SpO2:  %\n"),
        SNIPPET_MEDS("Meds",
                "# Medications\n- \n"),
        SNIPPET_ALLERGY("Allergy",
                "# Allergy\n- NKDA\n"),
        SNIPPET_ASSESS("Assessment",
                "# Assessment\n- \n"),
        SNIPPET_PLAN("Plan",
                "# Plan\n- \n"),
        SNIPPET_FOLLOWUP("Follow-up",
                "# Follow-up\n- Return in  weeks\n"),
        SNIPPET_SIGNATURE("Signature",
                "# Signature\nMigoJJ, MD\nEndocrinology\n");

        private final String display;
        private final String body;
        TemplateLibrary(String display, String body) { this.display = display; this.body = body; }
        public String displayName() { return display; }
        public String body() { return body; }
    }

    // ===== Formatting utilities =====
    private static class Formatter {
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