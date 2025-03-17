package com.richedd.clickupgui;

import com.richedd.clickupgui.config.ConfigurationManager;
import com.richedd.clickupgui.service.ClickUpService;
import com.richedd.clickupgui.ui.ConfigurationDialog;
import com.richedd.clickupgui.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Optional;

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
            ConfigurationDialog dialog = new ConfigurationDialog();
            Optional<String> result = dialog.showAndWait();
            
            if (result.isEmpty()) {
                System.exit(0);
            }
            
            try {
                configManager.setApiKey(result.get());
                configManager.saveConfiguration();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Configuration Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to save configuration: " + e.getMessage());
                alert.showAndWait();
                System.exit(1);
            }
        }

        // Initialize the ClickUp service with the API key
        clickUpService = new ClickUpService(configManager.getApiKey());

        // Create the main view
        MainView mainView = new MainView(clickUpService, primaryStage);
        Scene scene = new Scene(mainView, 1200, 800);
        
        primaryStage.setTitle("ClickUp GUI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
} 