package com.richedd.clickupgui.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;

public class ConfigurationManager {
    private static final String CONFIG_FILE = "clickup-config.json";
    private static final String API_KEY_PREF = "clickup.api.key";
    private static final String LAST_LIST_ID_PREF = "clickup.last.list";
    private final Preferences prefs;
    private final ObjectMapper objectMapper;
    private final Path configDir;

    public ConfigurationManager() {
        this.prefs = Preferences.userNodeForPackage(ConfigurationManager.class);
        this.objectMapper = new ObjectMapper();
        this.configDir = Path.of(System.getProperty("user.home"), ".clickup-gui");
    }

    public boolean isConfigured() {
        return getApiKey() != null && !getApiKey().isEmpty();
    }

    public String getApiKey() {
        return prefs.get(API_KEY_PREF, null);
    }

    public void setApiKey(String apiKey) {
        prefs.put(API_KEY_PREF, apiKey);
    }

    public String getLastUsedListId() {
        return prefs.get(LAST_LIST_ID_PREF, null);
    }

    public void setLastUsedListId(String listId) {
        prefs.put(LAST_LIST_ID_PREF, listId);
    }

    public void saveConfiguration() throws IOException {
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
    }

    public void clearConfiguration() {
        try {
            prefs.clear();
        } catch (Exception e) {
            // Log error or handle appropriately
        }
    }
} 