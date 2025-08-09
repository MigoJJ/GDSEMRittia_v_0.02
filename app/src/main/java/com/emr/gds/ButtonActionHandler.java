package com.emr.gds;

import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;

/**
 * Handles single and double-click events for Buttons.
 * This separates the action logic from the UI creation.
 */
public class ButtonActionHandler implements EventHandler<MouseEvent> {

    @Override
    public void handle(MouseEvent event) {
        // Ensure the event source is a Button before proceeding.
        if (!(event.getSource() instanceof Button button)) {
            return;
        }

        if (event.getClickCount() == 2) {
            // Double-click action
            System.out.println("Double-click on: " + button.getText());
            // TODO: Add specific double-click logic here
            
        } else if (event.getClickCount() == 1) {
            // Single-click action
            System.out.println("Single-click on: " + button.getText());
            // TODO: Add specific single-click logic here
        }
    }
}