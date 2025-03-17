package com.richedd.clickupgui.config;

public class ClickUpConfig {
    private static final String API_BASE_URL = "https://api.clickup.com/api/v2";
    private String apiKey;

    public ClickUpConfig(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiBaseUrl() {
        return API_BASE_URL;
    }
} 