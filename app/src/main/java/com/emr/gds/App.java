package com.emr.gds;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * JavaFX version of GDSEMR ITTIA prototype.
 * Top: 7 buttons, Bottom: 7 buttons
 * Left: one editable TextArea (with scroll)
 * Center: 10 editable TextAreas in a 5x2 grid (each with its own scroll)
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("GDSEMR ITTIA version 0.01 (JavaFX)");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8));

        // Top (North): 7 buttons
        HBox topBar = new HBox(8);
        topBar.setAlignment(Pos.CENTER_LEFT);
        for (int i = 1; i <= 7; i++) {
            Button btn = new Button("North " + i);
            attachClickHandler(btn);
            btn.setMinWidth(90);
            topBar.getChildren().add(btn);
        }
        root.setTop(topBar);

        // Bottom (South): 7 buttons
        HBox bottomBar = new HBox(8);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        for (int i = 1; i <= 7; i++) {
            Button btn = new Button("South " + i);
            attachClickHandler(btn);
            btn.setMinWidth(90);
            bottomBar.getChildren().add(btn);
        }
        root.setBottom(bottomBar);

        // Left (West): one TextArea with Scroll
        TextArea westArea = new TextArea();
        westArea.setPromptText("Left / West notes...");
        westArea.setWrapText(true);
        ScrollPane westScroll = new ScrollPane(westArea);
        westScroll.setFitToWidth(true);
        westScroll.setFitToHeight(true);
        westScroll.setPrefViewportWidth(260);
        root.setLeft(westScroll);

        // Center: 10 TextAreas in a 5x2 grid
        GridPane centerGrid = new GridPane();
        centerGrid.setHgap(8);
        centerGrid.setVgap(8);

        int rows = 5;
        int cols = 2;
        for (int i = 0; i < rows * cols; i++) {
            TextArea area = new TextArea();
            area.setPromptText("Area " + (i + 1));
            area.setWrapText(true);

            ScrollPane sp = new ScrollPane(area);
            sp.setFitToWidth(true);
            sp.setFitToHeight(true);
            sp.setPrefViewportHeight(140);
            sp.setPrefViewportWidth(300);

            int r = i / cols;
            int c = i % cols;
            centerGrid.add(sp, c, r);
        }
        root.setCenter(centerGrid);

        Scene scene = new Scene(root, 1200, 720);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Attach single/double click behavior for JavaFX Buttons.
     * Single-click: prints "Single-click on: <text>"
     * Double-click: prints "Double-click on: <text>"
     */
    private void attachClickHandler(Button button) {
        button.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() == 2) {
                System.out.println("Double-click on: " + button.getText());
                // TODO: add double-click action
            } else if (e.getClickCount() == 1) {
                System.out.println("Single-click on: " + button.getText());
                // TODO: add single-click action
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
