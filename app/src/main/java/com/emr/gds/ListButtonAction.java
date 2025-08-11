package com.emr.gds;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ListButtonAction {

    private final IttiaApp app;

    public ListButtonAction(IttiaApp app) {
        this.app = app;
    }

    public ToolBar buildTopBar() {
        Button btnInsertTemplate = new Button("Insert Template (Ctrl+I)");
        btnInsertTemplate.setOnAction(e -> app.insertTemplateIntoFocusedArea(TemplateLibrary.HPI));

        Button btnFormat = new Button("Auto Format (Ctrl+Shift+F)");
        btnFormat.setOnAction(e -> app.formatCurrentArea());

        Button btnCopyAll = new Button("Copy All (Ctrl+Shift+C)");
        btnCopyAll.setOnAction(e -> app.copyAllToClipboard());

        // Add the new DB Manager button to the top bar
        Button btnDbManager = new Button("DB Manager");
        btnDbManager.setOnAction(e -> app.showDbManager());

        // Templates menu
        MenuButton templatesMenu = new MenuButton("Templates");
        for (TemplateLibrary t : TemplateLibrary.values()) {
            MenuItem mi = new MenuItem(t.displayName());
            mi.setOnAction(e -> app.insertTemplateIntoFocusedArea(t));
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
                new Separator(), // Add a separator for better visual grouping
                btnDbManager,
                spacer,
                hint
        );
        return tb;
    }

    public ToolBar buildBottomBar() {
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
        b.setOnAction(e -> app.insertBlockIntoFocusedArea(snippet));
        return b;
    }

    // ===== Template library =====

    public enum TemplateLibrary {
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
}
