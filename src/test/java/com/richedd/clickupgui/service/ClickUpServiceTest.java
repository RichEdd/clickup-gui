package com.richedd.clickupgui.service;

import com.richedd.clickupgui.config.ClickUpConfig;
import com.richedd.clickupgui.exception.ClickUpException;
import com.richedd.clickupgui.model.Task;
import com.richedd.clickupgui.model.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class ClickUpServiceTest {
    private ClickUpService clickUpService;
    private static final String TEST_API_KEY = System.getenv("CLICKUP_API_KEY");
    private static final String TEST_LIST_ID = System.getenv("CLICKUP_TEST_LIST_ID");

    @BeforeEach
    void setUp() {
        // Skip tests if API key is not set
        if (TEST_API_KEY == null || TEST_API_KEY.isEmpty()) {
            throw new IllegalStateException("CLICKUP_API_KEY environment variable must be set to run tests");
        }
        clickUpService = new ClickUpService(new ClickUpConfig(TEST_API_KEY));
    }

    @Test
    void getWorkspaces_ShouldReturnWorkspaces() throws ExecutionException, InterruptedException {
        CompletableFuture<List<Workspace>> futureWorkspaces = clickUpService.getWorkspaces();
        List<Workspace> workspaces = futureWorkspaces.get();
        
        assertNotNull(workspaces);
        assertFalse(workspaces.isEmpty());
        assertNotNull(workspaces.get(0).getId());
        assertNotNull(workspaces.get(0).getName());
    }

    @Test
    void createAndDeleteTask_ShouldSucceed() throws ExecutionException, InterruptedException {
        // Skip test if list ID is not set
        if (TEST_LIST_ID == null || TEST_LIST_ID.isEmpty()) {
            throw new IllegalStateException("CLICKUP_TEST_LIST_ID environment variable must be set to run this test");
        }

        // Create a test task
        Task newTask = new Task();
        newTask.setName("Test Task " + System.currentTimeMillis());
        newTask.setDescription("This is a test task created by automated tests");

        // Create the task
        Task createdTask = clickUpService.createTask(TEST_LIST_ID, newTask).get();
        assertNotNull(createdTask);
        assertNotNull(createdTask.getId());
        assertEquals(newTask.getName(), createdTask.getName());

        // Delete the task
        CompletableFuture<Void> deleteFuture = clickUpService.deleteTask(createdTask.getId());
        assertDoesNotThrow(() -> deleteFuture.get());
    }

    @Test
    void invalidApiKey_ShouldThrowException() {
        ClickUpService invalidService = new ClickUpService(new ClickUpConfig("invalid_key"));
        CompletableFuture<List<Workspace>> future = invalidService.getWorkspaces();
        
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof ClickUpException);
        assertEquals(401, ((ClickUpException) exception.getCause()).getStatusCode());
    }
} 