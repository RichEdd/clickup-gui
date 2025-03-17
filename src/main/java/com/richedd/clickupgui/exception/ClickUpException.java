package com.richedd.clickupgui.exception;

public class ClickUpException extends RuntimeException {
    private final int statusCode;
    private final String errorBody;

    public ClickUpException(String message, int statusCode, String errorBody) {
        super(message);
        this.statusCode = statusCode;
        this.errorBody = errorBody;
    }

    public ClickUpException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.errorBody = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorBody() {
        return errorBody;
    }
} 