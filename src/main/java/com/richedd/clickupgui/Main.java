package com.richedd.clickupgui;

import com.richedd.clickupgui.config.ConfigurationManager;
import com.richedd.clickupgui.config.ClickUpConfig;
import com.richedd.clickupgui.service.ClickUpService;
import com.richedd.clickupgui.ui.ConfigurationDialog;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {
    private ConfigurationManager configManager;
    private ClickUpService clickUpService;

    @Override
    public void init() {
        configManager = new ConfigurationManager();
    }

    @Override
    public void start(Stage primaryStage) {
        // Check if the application is configured
        if (!configManager.isConfigured()) {
            boolean configured = ConfigurationDialog.showAndWait(configManager);
            if (!configured) {
                // User cancelled configuration
                System.exit(0);
                return;
            }
        }

        // Initialize ClickUp service with the configured API key
        clickUpService = new ClickUpService(new ClickUpConfig(configManager.getApiKey()));

        // Create the main window
        Label label = new Label("ClickUp GUI");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 800, 600);
        
        primaryStage.setTitle("ClickUp GUI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
} 