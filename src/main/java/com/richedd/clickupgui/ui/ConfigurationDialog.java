package com.richedd.clickupgui.ui;

import javafx.application.HostServices;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.net.URI;

public class ConfigurationDialog extends Dialog<String> {
    private final TextField apiKeyField;

    public ConfigurationDialog() {
        setTitle("ClickUp Configuration");
        setHeaderText("Please enter your ClickUp API key");
        setContentText("You can find your API key in ClickUp settings under Apps > API Token.");

        // Create the API key input field
        apiKeyField = new TextField();
        apiKeyField.setPromptText("Enter your API key");

        // Create the dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("API Key:"), 0, 0);
        grid.add(apiKeyField, 1, 0);

        // Add hyperlink to ClickUp API documentation
        Hyperlink link = new Hyperlink("How to get your API key?");
        link.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://clickup.com/api"));
            } catch (Exception ex) {
                // Silently fail if the browser cannot be opened
            }
        });
        grid.add(link, 1, 1);

        getDialogPane().setContent(grid);

        // Add buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Enable/Disable save button depending on whether an API key was entered
        Node saveButton = getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        apiKeyField.textProperty().addListener((observable, oldValue, newValue) -> 
            saveButton.setDisable(newValue.trim().isEmpty())
        );

        // Convert the result to String when the save button is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return apiKeyField.getText().trim();
            }
            return null;
        });
    }
} 