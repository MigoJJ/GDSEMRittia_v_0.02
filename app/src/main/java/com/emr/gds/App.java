package com.emr.gds;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * JavaFX version of GDSEMR ITTIA prototype.
 * This class is now responsible for assembling the UI components
 * created by the ViewFactory.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("GDSEMR ITTIA version 0.01 (JavaFX - Refactored)");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8));

        // 1. Create a single action handler instance to be shared by all buttons
        EventHandler<MouseEvent> buttonActionHandler = new ButtonActionHandler();

        // 2. Use the factory to create UI components
        root.setTop(ViewFactory.createButtonBar(7, "North", buttonActionHandler));
        root.setBottom(ViewFactory.createButtonBar(7, "South", buttonActionHandler));
        root.setLeft(ViewFactory.createWestArea());
        root.setCenter(ViewFactory.createCenterGrid(5, 2));

        // 3. Set up the scene and show the stage
        Scene scene = new Scene(root, 1200, 720);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}