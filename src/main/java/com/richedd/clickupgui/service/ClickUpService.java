package com.richedd.clickupgui.service;

import com.richedd.clickupgui.model.Task;
import com.richedd.clickupgui.model.Workspace;
import com.richedd.clickupgui.model.SpaceWithLists;
import com.richedd.clickupgui.model.TaskList;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

public class ClickUpService {
    private static final String API_BASE_URL = "https://api.clickup.com/api/v2";
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ClickUpService(String apiKey) {
        this.apiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : "pk_132021316_3Y2JWD1NM4GGY3RV63JJ01PFUA9PQCQJ";
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    }

    public CompletableFuture<List<SpaceWithLists>> getSpacesForTeam(String teamId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/team/" + teamId + "/space?archived=false"))
            .header("Authorization", apiKey)
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    System.out.println("Response: " + response.body());  // Debug output
                    JsonNode root = objectMapper.readTree(response.body());
                    return objectMapper.convertValue(root.get("spaces"),
                        new TypeReference<List<SpaceWithLists>>() {});
                } catch (IOException e) {
                    throw new RuntimeException("Failed to parse spaces", e);
                }
            });
    }

    public CompletableFuture<List<Workspace>> getWorkspaces() {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/team"))
            .header("Authorization", apiKey)
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    JsonNode root = objectMapper.readTree(response.body());
                    return objectMapper.convertValue(root.get("teams"), 
                        new TypeReference<List<Workspace>>() {});
                } catch (IOException e) {
                    throw new RuntimeException("Failed to parse workspaces", e);
                }
            });
    }

    public CompletableFuture<List<Task>> getTasksForList(String listId) {
        System.out.println("Fetching tasks for list ID: " + listId);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/list/" + listId + "/task?archived=false"))
            .header("Authorization", apiKey)
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    System.out.println("Response status code: " + response.statusCode());
                    if (response.statusCode() != 200) {
                        String errorBody = response.body();
                        System.err.println("Error response body: " + errorBody);
                        throw new RuntimeException("Failed to get tasks. Status: " + response.statusCode() + ", Body: " + errorBody);
                    }

                    JsonNode root = objectMapper.readTree(response.body());
                    if (!root.has("tasks")) {
                        System.err.println("Response does not contain 'tasks' field: " + response.body());
                        throw new RuntimeException("Invalid response format: missing 'tasks' field");
                    }

                    try {
                        // Configure ObjectMapper to handle nested objects
                        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        
                        List<Task> tasks = objectMapper.convertValue(root.get("tasks"), 
                            new TypeReference<List<Task>>() {});
                        System.out.println("Successfully loaded " + tasks.size() + " tasks from list " + listId);
                        return tasks;
                    } catch (Exception e) {
                        System.err.println("Deserialization error: " + e.getMessage());
                        System.err.println("Stack trace:");
                        e.printStackTrace();
                        throw new RuntimeException("Failed to deserialize tasks: " + e.getMessage(), e);
                    }
                } catch (IOException e) {
                    System.err.println("Error parsing tasks response: " + e.getMessage());
                    throw new RuntimeException("Failed to parse tasks: " + e.getMessage(), e);
                }
            });
    }

    public CompletableFuture<Task> createTask(String listId, Task task) {
        String json;
        try {
            json = objectMapper.writeValueAsString(task);
        } catch (IOException e) {
            CompletableFuture<Task> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/list/" + listId + "/task"))
            .header("Authorization", apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    return objectMapper.readValue(response.body(), Task.class);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to parse created task", e);
                }
            });
    }

    public CompletableFuture<Task> updateTask(String taskId, Task task) {
        String json;
        try {
            json = objectMapper.writeValueAsString(task);
        } catch (IOException e) {
            CompletableFuture<Task> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/task/" + taskId))
            .header("Authorization", apiKey)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    return objectMapper.readValue(response.body(), Task.class);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to parse updated task", e);
                }
            });
    }

    public CompletableFuture<Void> deleteTask(String taskId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/task/" + taskId))
            .header("Authorization", apiKey)
            .DELETE()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Failed to delete task: " + response.body());
                }
                return null;
            });
    }

    public CompletableFuture<List<SpaceWithLists>> getSpacesWithLists(String workspaceId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/team/" + workspaceId + "/space?archived=false"))
            .header("Authorization", apiKey)
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenCompose(response -> {
                try {
                    JsonNode root = objectMapper.readTree(response.body());
                    List<SpaceWithLists> spaces = objectMapper.convertValue(root.get("spaces"),
                        new TypeReference<List<SpaceWithLists>>() {});
                    
                    // Create a list of futures for getting lists for each space
                    List<CompletableFuture<SpaceWithLists>> futures = spaces.stream()
                        .map(space -> getListsForSpace(space.getId())
                            .thenApply(lists -> {
                                space.setLists(lists);
                                return space;
                            }))
                        .toList();
                    
                    // Wait for all futures to complete
                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .thenApply(v -> spaces);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to parse spaces", e);
                }
            });
    }

    private CompletableFuture<List<TaskList>> getListsForSpace(String spaceId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/space/" + spaceId + "/list?archived=false"))
            .header("Authorization", apiKey)
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    JsonNode root = objectMapper.readTree(response.body());
                    return objectMapper.convertValue(root.get("lists"),
                        new TypeReference<List<TaskList>>() {});
                } catch (IOException e) {
                    throw new RuntimeException("Failed to parse lists", e);
                }
            });
    }

    public CompletableFuture<TaskList> createList(String spaceId, String name, String content) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                "name", name,
                "content", content
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/space/" + spaceId + "/list"))
                .header("Authorization", "pk_132021316_3Y2JWD1NM4GGY3RV63JJ01PFUA9PQCQJ")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        System.out.println("Response: " + response.body());
                        if (response.statusCode() != 200) {
                            throw new RuntimeException("Failed to create list. Status: " + response.statusCode() + ", Body: " + response.body());
                        }
                        return objectMapper.readValue(response.body(), TaskList.class);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse created list", e);
                    }
                });
        } catch (IOException e) {
            CompletableFuture<TaskList> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
} 