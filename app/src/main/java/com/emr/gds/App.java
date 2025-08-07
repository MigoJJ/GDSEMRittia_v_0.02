package com.emr.gds;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * A GUI application that displays a frame with several panels and interactive components.
 * This version replaces the original console application.
 */
public class App {

    /**
     * Creates and shows the main GUI. This method should be called on the
     * Event Dispatch Thread (EDT).
     */
    private static void createAndShowGui() {
        // 1. Create the main frame with the specified title.
        JFrame frame = new JFrame("GDSEMR ITTIA version 0.01");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Use BorderLayout with 5px horizontal and vertical gaps between components.
        frame.setLayout(new BorderLayout(5, 5));

        // 2. Create the North panel with 7 buttons.
        // GridLayout creates a grid of 1 row and 7 columns.
        JPanel northPanel = new JPanel(new GridLayout(1, 7, 5, 5));
        for (int i = 1; i <= 7; i++) {
            JButton button = new JButton("North " + i);
            addClickListener(button); // Add logic for single/double clicks.
            northPanel.add(button);
        }
        frame.add(northPanel, BorderLayout.NORTH);

        // 3. Create the South panel with 7 buttons.
        JPanel southPanel = new JPanel(new GridLayout(1, 7, 5, 5));
        for (int i = 1; i <= 7; i++) {
            JButton button = new JButton("South " + i);
            addClickListener(button); // Add logic for single/double clicks.
            southPanel.add(button);
        }
        frame.add(southPanel, BorderLayout.SOUTH);

        // 4. Create the West panel with one editable JTextArea.
        // JScrollPane is added to allow scrolling if the text exceeds the area's size.
        JTextArea westTextArea = new JTextArea(10, 20); // Initial size: 10 rows, 20 columns
        JScrollPane westScrollPane = new JScrollPane(westTextArea);
        frame.add(westScrollPane, BorderLayout.WEST);

        // 5. Create the Central panel with 10 editable JTextAreas.
        // A 5x2 GridLayout is used to arrange the 10 text areas neatly.
        JPanel centerPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        for (int i = 1; i <= 10; i++) {
            JTextArea textArea = new JTextArea(5, 15); // Smaller text areas for the center
            JScrollPane scrollPane = new JScrollPane(textArea); // Each gets its own scrollbar
            centerPanel.add(scrollPane);
        }
        frame.add(centerPanel, BorderLayout.CENTER);

        // 6. Finalize and display the window.
        frame.pack(); // Adjusts window size to fit all components.
        frame.setLocationRelativeTo(null); // Centers the window on the screen.
        frame.setVisible(true);
    }

    /**
     * Adds a mouse listener to a button to handle single and double clicks.
     * An output is printed to the console to demonstrate the functionality.
     *
     * @param button The JButton to attach the listener to.
     */
    private static void addClickListener(JButton button) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Check the number of clicks
                if (e.getClickCount() == 2) {
                    System.out.println("Double-click on: " + button.getText());
                    // TODO: Add double-click action here
                } else {
                    System.out.println("Single-click on: " + button.getText());
                    // TODO: Add single-click action here
                }
            }
        });
    }

    /**
     * The main entry point of the application.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // It's best practice to create and show GUIs on the Event Dispatch Thread (EDT).
        // SwingUtilities.invokeLater ensures this.
        SwingUtilities.invokeLater(App::createAndShowGui);
    }
}
