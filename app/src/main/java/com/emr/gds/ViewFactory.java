package com.emr.gds;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/**
 * A factory class for creating reusable UI components for the application.
 */
public class ViewFactory {

    /**
     * Creates a horizontal bar of buttons.
     *
     * @param count The number of buttons to create.
     * @param textPrefix The prefix for the button text (e.g., "North").
     * @param actionHandler The event handler for button clicks.
     * @return An HBox containing the configured buttons.
     */
    public static HBox createButtonBar(int count, String textPrefix, EventHandler<MouseEvent> actionHandler) {
        HBox buttonBar = new HBox(8); // Spacing between buttons
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        for (int i = 1; i <= count; i++) {
            Button btn = new Button(textPrefix + " " + i);
            btn.setMinWidth(90);
            btn.addEventHandler(MouseEvent.MOUSE_CLICKED, actionHandler); // Attach the shared handler
            buttonBar.getChildren().add(btn);
        }
        return buttonBar;
    }

    /**
     * Creates the central grid of TextAreas.
     *
     * @param rows The number of rows in the grid.
     * @param cols The number of columns in the grid.
     * @return A GridPane populated with TextAreas.
     */
    public static GridPane createCenterGrid(int rows, int cols) {
        GridPane centerGrid = new GridPane();
        centerGrid.setHgap(8);
        centerGrid.setVgap(8);

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
        return centerGrid;
    }

    /**
     * Creates the scrollable text area for the west region.
     *
     * @return A ScrollPane containing the configured TextArea.
     */
    public static ScrollPane createWestArea() {
        TextArea westArea = new TextArea();
        westArea.setPromptText("Left / West notes...");
        westArea.setWrapText(true);
        
        ScrollPane westScroll = new ScrollPane(westArea);
        westScroll.setFitToWidth(true);
        westScroll.setFitToHeight(true);
        westScroll.setPrefViewportWidth(260);
        return westScroll;
    }
}