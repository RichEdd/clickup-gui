package com.richedd.clickupgui.ui;

import com.richedd.clickupgui.model.Task;
import com.richedd.clickupgui.model.Status;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class TaskListCell extends ListCell<Task> {
    private final VBox content;
    private final Label nameLabel;
    private final Label statusLabel;
    private final Label descriptionLabel;
    private final Label dueDateLabel;

    public TaskListCell() {
        content = new VBox(5);
        content.setPadding(new Insets(5, 10, 5, 10));
        
        // Task name
        nameLabel = new Label();
        nameLabel.setFont(Font.font(null, FontWeight.BOLD, 14));
        nameLabel.setWrapText(true);
        
        // Status with background
        statusLabel = new Label();
        statusLabel.setPadding(new Insets(2, 8, 2, 8));
        statusLabel.setStyle("-fx-background-radius: 3;");
        
        // Description (truncated)
        descriptionLabel = new Label();
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(300);
        descriptionLabel.setStyle("-fx-text-fill: #666666;");
        
        // Due date
        dueDateLabel = new Label();
        dueDateLabel.setStyle("-fx-text-fill: #666666;");
        
        // Top row with name and status
        HBox topRow = new HBox(10);
        topRow.getChildren().addAll(nameLabel, statusLabel);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        
        content.getChildren().addAll(topRow, descriptionLabel, dueDateLabel);
        
        // Add a border and background
        content.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: white; -fx-background-radius: 5;");
    }

    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);
        
        if (empty || task == null) {
            setGraphic(null);
        } else {
            // Update name
            nameLabel.setText(task.getName() != null ? task.getName() : "Untitled Task");
            
            // Update status with color
            Status status = task.getStatus();
            String statusText = status != null ? status.getStatus() : "Open";
            statusLabel.setText(statusText);
            updateStatusStyle(status);
            
            // Update description (truncate if too long)
            String description = task.getDescription();
            if (description != null && !description.isEmpty()) {
                if (description.length() > 100) {
                    description = description.substring(0, 97) + "...";
                }
                descriptionLabel.setText(description);
                descriptionLabel.setVisible(true);
            } else {
                descriptionLabel.setVisible(false);
            }
            
            // Update due date if present
            if (task.getDueDate() != null) {
                dueDateLabel.setText("Due: " + task.getDueDate()
                    .atZone(java.time.ZoneOffset.UTC)
                    .toLocalDateTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")));
                dueDateLabel.setVisible(true);
            } else {
                dueDateLabel.setVisible(false);
            }
            
            setGraphic(content);
        }
    }

    private void updateStatusStyle(Status status) {
        String backgroundColor;
        String textColor;
        
        if (status == null || status.getStatus() == null) {
            backgroundColor = "#e0e0e0";
            textColor = "#000000";
        } else {
            // Use the color from the status object if available, otherwise use default colors
            backgroundColor = status.getColor() != null ? status.getColor() : "#e0e0e0";
            textColor = isLightColor(backgroundColor) ? "#000000" : "#ffffff";
        }
        
        statusLabel.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 3;",
            backgroundColor, textColor));
    }

    private boolean isLightColor(String color) {
        if (color.startsWith("#")) {
            color = color.substring(1);
        }
        
        // Parse the color components
        int r = Integer.parseInt(color.substring(0, 2), 16);
        int g = Integer.parseInt(color.substring(2, 4), 16);
        int b = Integer.parseInt(color.substring(4, 6), 16);
        
        // Calculate relative luminance
        double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
        
        return luminance > 0.5;
    }
} 