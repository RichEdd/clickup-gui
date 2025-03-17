package com.richedd.clickupgui.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richedd.clickupgui.config.ClickUpConfig;
import com.richedd.clickupgui.model.Task;
import okhttp3.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClickUpService {
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final ClickUpConfig config;

    public ClickUpService(ClickUpConfig config) {
        this.config = config;
        this.client = new OkHttpClient.Builder().build();
        this.objectMapper = new ObjectMapper();
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

    private <T> CompletableFuture<T> makeAsyncRequest(Request request, TypeReference<T> typeReference) {
        CompletableFuture<T> future = new CompletableFuture<>();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("Unexpected response " + response));
                        return;
                    }

                    String json = responseBody.string();
                    T result = objectMapper.readValue(json, typeReference);
                    future.complete(result);
                }
            }
        });

        return future;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    // Response wrapper class for ClickUp API
    private static class ApiResponse<T> {
        public T tasks;
        public T task;
    }
} 