package com.richedd.clickupgui.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.richedd.clickupgui.config.ClickUpConfig;
import com.richedd.clickupgui.exception.ClickUpException;
import com.richedd.clickupgui.model.Task;
import com.richedd.clickupgui.model.Workspace;
import okhttp3.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ClickUpService {
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final ClickUpConfig config;

    public ClickUpService(ClickUpConfig config) {
        this.config = config;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
            
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    }

    public CompletableFuture<List<Workspace>> getWorkspaces() {
        Request request = new Request.Builder()
            .url(config.getApiBaseUrl() + "/team")
            .addHeader("Authorization", config.getApiKey())
            .build();

        return makeAsyncRequest(request, new TypeReference<ApiResponse<List<Workspace>>>() {})
            .thenApply(response -> response.teams);
    }

    public CompletableFuture<List<Task>> getTasksForList(String listId) {
        Request request = new Request.Builder()
            .url(config.getApiBaseUrl() + "/list/" + listId + "/task")
            .addHeader("Authorization", config.getApiKey())
            .build();

        return makeAsyncRequest(request, new TypeReference<ApiResponse<List<Task>>>() {})
            .thenApply(response -> response.tasks);
    }

    public CompletableFuture<Task> createTask(String listId, Task task) {
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"),
            toJson(task)
        );

        Request request = new Request.Builder()
            .url(config.getApiBaseUrl() + "/list/" + listId + "/task")
            .addHeader("Authorization", config.getApiKey())
            .post(body)
            .build();

        return makeAsyncRequest(request, new TypeReference<ApiResponse<Task>>() {})
            .thenApply(response -> response.task);
    }

    public CompletableFuture<Task> updateTask(String taskId, Task task) {
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"),
            toJson(task)
        );

        Request request = new Request.Builder()
            .url(config.getApiBaseUrl() + "/task/" + taskId)
            .addHeader("Authorization", config.getApiKey())
            .put(body)
            .build();

        return makeAsyncRequest(request, new TypeReference<ApiResponse<Task>>() {})
            .thenApply(response -> response.task);
    }

    public CompletableFuture<Void> deleteTask(String taskId) {
        Request request = new Request.Builder()
            .url(config.getApiBaseUrl() + "/task/" + taskId)
            .addHeader("Authorization", config.getApiKey())
            .delete()
            .build();

        return makeAsyncRequest(request, new TypeReference<Void>() {});
    }

    private <T> CompletableFuture<T> makeAsyncRequest(Request request, TypeReference<T> typeReference) {
        CompletableFuture<T> future = new CompletableFuture<>();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(new ClickUpException("Network error", e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String responseString = responseBody != null ? responseBody.string() : null;
                    
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new ClickUpException(
                            "API error: " + response.code() + " " + response.message(),
                            response.code(),
                            responseString
                        ));
                        return;
                    }

                    if (typeReference.getType() == Void.class) {
                        future.complete(null);
                        return;
                    }

                    T result = objectMapper.readValue(responseString, typeReference);
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(new ClickUpException("Failed to process response", e));
                }
            }
        });

        return future;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new ClickUpException("Failed to serialize object to JSON", e);
        }
    }

    // Response wrapper class for ClickUp API
    private static class ApiResponse<T> {
        public T tasks;
        public T task;
        public T teams;
    }
} 