package com.richedd.clickupgui.ui;

import com.richedd.clickupgui.config.ConfigurationManager;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import java.util.Optional;

public class ConfigurationDialog extends Dialog<Boolean> {
    private final TextField apiKeyField;
    private final ConfigurationManager configManager;

    public ConfigurationDialog(ConfigurationManager configManager) {
        this.configManager = configManager;
        
        // Dialog setup
        setTitle("ClickUp Configuration");
        setHeaderText("Please enter your ClickUp API key to get started.");
        
        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the API key field
        apiKeyField = new TextField();
        apiKeyField.setPromptText("Enter your ClickUp API key");
        
        // Create help text with instructions
        TextArea helpText = new TextArea(
            "To get your API key:\n" +
            "1. Log in to ClickUp\n" +
            "2. Go to Settings\n" +
            "3. Click on 'Apps'\n" +
            "4. Find 'API Token' section\n" +
            "5. Click 'Generate' or copy your existing API key"
        );
        helpText.setEditable(false);
        helpText.setWrapText(true);
        helpText.setPrefRowCount(6);
        helpText.setMaxWidth(Double.MAX_VALUE);
        
        // Create the layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("API Key:"), 0, 0);
        grid.add(apiKeyField, 1, 0);
        GridPane.setHgrow(apiKeyField, Priority.ALWAYS);
        
        grid.add(new Label("Instructions:"), 0, 1);
        grid.add(helpText, 1, 1);

        getDialogPane().setContent(grid);
        getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        
        // Enable/Disable save button depending on whether API key was entered
        Node saveButton = getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        apiKeyField.textProperty().addListener((observable, oldValue, newValue) -> 
            saveButton.setDisable(newValue.trim().isEmpty())
        );

        // Set the result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String apiKey = apiKeyField.getText().trim();
                if (!apiKey.isEmpty()) {
                    configManager.setApiKey(apiKey);
                    try {
                        configManager.saveConfiguration();
                        return true;
                    } catch (Exception e) {
                        showError("Failed to save configuration: " + e.getMessage());
                    }
                }
            }
            return false;
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Configuration Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean showAndWait(ConfigurationManager configManager) {
        ConfigurationDialog dialog = new ConfigurationDialog(configManager);
        Optional<Boolean> result = dialog.showAndWait();
        return result.orElse(false);
    }
} 