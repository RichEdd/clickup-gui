package com.richedd.clickupgui.ui;

import com.richedd.clickupgui.model.Task;
import com.richedd.clickupgui.model.Status;
import com.richedd.clickupgui.model.Workspace;
import com.richedd.clickupgui.model.SpaceWithLists;
import com.richedd.clickupgui.model.TaskList;
import com.richedd.clickupgui.service.ClickUpService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.application.Platform;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Comparator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.FileChooser.ExtensionFilter;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public class MainView extends BorderPane {
    private final ClickUpService clickUpService;
    private final Stage stage;
    private TreeView<NavigationItem> navigationTree;
    private ListView<Task> taskList;
    private VBox taskDetailPanel;
    private Task selectedTask;
    private ComboBox<String> statusFilter;
    private ComboBox<String> sortBy;
    private TextField searchField;
    private List<Task> originalTasks;

    public MainView(ClickUpService clickUpService, Stage stage) {
        this.clickUpService = clickUpService;
        this.stage = stage;
        setupUI();
        loadWorkspaces();
    }

    private void setupUI() {
        // Top: Menu Bar
        setupMenuBar();
        
        // Left: Navigation Panel
        setupNavigationPanel();
        
        // Center: Task List
        setupTaskList();
        
        // Right: Task Details
        setupTaskDetailPanel();
    }

    private void setupMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.setOnAction(e -> showSettings());
        MenuItem createDevCategoriesItem = new MenuItem("Create Development Categories");
        createDevCategoriesItem.setOnAction(e -> createDevelopmentCategoriesLists());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> handleExit());
        fileMenu.getItems().addAll(settingsItem, createDevCategoriesItem, new SeparatorMenuItem(), exitItem);
        
        // Help Menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());
        helpMenu.getItems().add(aboutItem);
        
        menuBar.getMenus().addAll(fileMenu, helpMenu);
        setTop(menuBar);
    }

    private void handleExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Exit");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("Any unsaved changes will be lost.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Platform.exit();
        }
    }

    private void showSettings() {
        // TODO: Implement settings dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText(null);
        alert.setContentText("Settings dialog will be implemented soon.");
        alert.showAndWait();
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About ClickUp GUI");
        alert.setHeaderText("ClickUp GUI");
        alert.setContentText("A desktop application for managing ClickUp tasks.\nVersion 1.0");
        alert.showAndWait();
    }

    private void createDevelopmentCategoriesLists() {
        // Get spaces from the team first
        clickUpService.getSpacesForTeam("9013790997")
            .thenAccept(spaces -> {
                Platform.runLater(() -> {
                    for (SpaceWithLists space : spaces) {
                        clickUpService.createList(space.getId(), "Development Categories", "Container for development task categories")
                            .thenAccept(list -> {
                                Platform.runLater(() -> {
                                    showSuccess("List Created", "Development Categories list created in space " + space.getName());
                                    // Refresh the navigation tree to show the new list
                                    loadWorkspaces();
                                });
                            })
                            .exceptionally(throwable -> {
                                Platform.runLater(() -> {
                                    showError("Error Creating List", "Failed to create list in space " + space.getName() + ": " + throwable.getMessage());
                                });
                                return null;
                            });
                    }
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showError("Error Getting Spaces", "Failed to get spaces: " + throwable.getMessage());
                });
                return null;
            });
    }

    private void setupNavigationPanel() {
        navigationTree = new TreeView<>();
        navigationTree.setShowRoot(false);
        navigationTree.setPrefWidth(250);
        navigationTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(NavigationItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        navigationTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                NavigationItem item = newVal.getValue();
                switch (item.getType()) {
                    case LIST:
                        System.out.println("Loading tasks for list: " + item.getName() + " (" + item.getId() + ")");
                        loadTasksForList(item.getId());
                        break;
                    case SPACE:
                        // Show space details and its lists
                        taskList.getItems().clear();
                        selectedTask = null;
                        updateTaskDetailPanel();
                        
                        // Create a VBox for space content
                        VBox spaceContent = new VBox(10);
                        spaceContent.setPadding(new Insets(20));
                        spaceContent.setStyle("-fx-background-color: white;");
                        
                        // Space header
                        Label spaceTitle = new Label(item.getName());
                        spaceTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
                        
                        // Lists section
                        Label listsLabel = new Label("Lists");
                        listsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
                        
                        // Create a VBox for lists
                        VBox listsContainer = new VBox(10);
                        listsContainer.setPadding(new Insets(10));
                        
                        // Add each list as a clickable item
                        TreeItem<NavigationItem> spaceItem = newVal;
                        for (TreeItem<NavigationItem> listItem : spaceItem.getChildren()) {
                            NavigationItem listNav = listItem.getValue();
                            
                            // Create a list item container
                            HBox listItemBox = new HBox(10);
                            listItemBox.setPadding(new Insets(10));
                            listItemBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;");
                            listItemBox.setAlignment(Pos.CENTER_LEFT);
                            
                            // List name
                            Label listName = new Label(listNav.getName());
                            listName.setStyle("-fx-font-size: 14px;");
                            
                            // View button
                            Button viewButton = new Button("View Tasks");
                            viewButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                            viewButton.setOnAction(e -> loadTasksForList(listNav.getId()));
                            
                            listItemBox.getChildren().addAll(listName, new Region(), viewButton);
                            HBox.setHgrow(listItemBox.getChildren().get(1), Priority.ALWAYS);
                            
                            // Add hover effect
                            listItemBox.setOnMouseEntered(e -> 
                                listItemBox.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 5;"));
                            listItemBox.setOnMouseExited(e -> 
                                listItemBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;"));
                            
                            listsContainer.getChildren().add(listItemBox);
                        }
                        
                        // Add all components to the space content
                        spaceContent.getChildren().addAll(
                            spaceTitle,
                            new Separator(),
                            listsLabel,
                            listsContainer
                        );
                        
                        // Set the space content as the center content
                        setCenter(spaceContent);
                        break;
                    case WORKSPACE:
                        // Show workspace details
                        taskList.getItems().clear();
                        selectedTask = null;
                        updateTaskDetailPanel();
                        
                        // Create a VBox for workspace content
                        VBox workspaceContent = new VBox(10);
                        workspaceContent.setPadding(new Insets(20));
                        workspaceContent.setStyle("-fx-background-color: white;");
                        
                        // Workspace header
                        Label workspaceTitle = new Label(item.getName());
                        workspaceTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
                        
                        // Spaces section
                        Label spacesLabel = new Label("Spaces");
                        spacesLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
                        
                        // Create a VBox for spaces
                        VBox spacesContainer = new VBox(10);
                        spacesContainer.setPadding(new Insets(10));
                        
                        // Add each space as a clickable item
                        TreeItem<NavigationItem> workspaceItem = newVal;
                        for (TreeItem<NavigationItem> spaceTreeItem : workspaceItem.getChildren()) {
                            NavigationItem spaceNav = spaceTreeItem.getValue();
                            
                            // Create a space item container
                            HBox spaceItemBox = new HBox(10);
                            spaceItemBox.setPadding(new Insets(10));
                            spaceItemBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;");
                            spaceItemBox.setAlignment(Pos.CENTER_LEFT);
                            
                            // Space name
                            Label spaceName = new Label(spaceNav.getName());
                            spaceName.setStyle("-fx-font-size: 14px;");
                            
                            // View button
                            Button viewButton = new Button("View Space");
                            viewButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                            viewButton.setOnAction(e -> navigationTree.getSelectionModel().select(spaceTreeItem));
                            
                            spaceItemBox.getChildren().addAll(spaceName, new Region(), viewButton);
                            HBox.setHgrow(spaceItemBox.getChildren().get(1), Priority.ALWAYS);
                            
                            // Add hover effect
                            spaceItemBox.setOnMouseEntered(e -> 
                                spaceItemBox.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 5;"));
                            spaceItemBox.setOnMouseExited(e -> 
                                spaceItemBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;"));
                            
                            spacesContainer.getChildren().add(spaceItemBox);
                        }
                        
                        // Add all components to the workspace content
                        workspaceContent.getChildren().addAll(
                            workspaceTitle,
                            new Separator(),
                            spacesLabel,
                            spacesContainer
                        );
                        
                        // Set the workspace content as the center content
                        setCenter(workspaceContent);
                        break;
                }
            }
        });

        VBox navigationPanel = new VBox(10);
        Label navLabel = new Label("Navigation");
        navLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        navigationPanel.getChildren().addAll(navLabel, navigationTree);
        navigationPanel.setPadding(new Insets(10));
        setLeft(navigationPanel);
    }

    private void setupTaskList() {
        taskList = new ListView<>();
        taskList.setCellFactory(lv -> new com.richedd.clickupgui.ui.TaskListCell());
        taskList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedTask = newVal;
            updateTaskDetailPanel();
        });

        // Create toolbar for task list
        ToolBar taskToolbar = new ToolBar();
        taskToolbar.setStyle("-fx-background-color: #f5f5f5;");
        
        // Add new task button
        Button newTaskButton = new Button("New Task");
        newTaskButton.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        newTaskButton.setOnAction(e -> createNewTask());
        
        // Add refresh button
        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-font-size: 14px;");
        refreshButton.setOnAction(e -> refreshCurrentView());
        
        // Add filter controls
        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All", "Open", "In Progress", "Review", "Completed");
        statusFilter.setValue("All");
        statusFilter.setPromptText("Filter by Status");
        statusFilter.setStyle("-fx-font-size: 14px;");
        statusFilter.setOnAction(e -> filterAndSortTasks());
        
        // Add sort controls
        sortBy = new ComboBox<>();
        sortBy.getItems().addAll("Name", "Status", "Due Date");
        sortBy.setValue("Name");
        sortBy.setPromptText("Sort by");
        sortBy.setStyle("-fx-font-size: 14px;");
        sortBy.setOnAction(e -> filterAndSortTasks());
        
        // Add search field
        searchField = new TextField();
        searchField.setPromptText("Search tasks...");
        searchField.setStyle("-fx-font-size: 14px;");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterAndSortTasks());
        searchField.setPrefWidth(200);
        
        // Create labels with styling
        Label filterLabel = new Label("Filter:");
        filterLabel.setStyle("-fx-font-size: 14px;");
        Label sortLabel = new Label("Sort:");
        sortLabel.setStyle("-fx-font-size: 14px;");
        
        // Add spacing between controls
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        
        taskToolbar.getItems().addAll(
            newTaskButton,
            new Separator(),
            refreshButton,
            spacer1,
            filterLabel,
            statusFilter,
            new Separator(),
            sortLabel,
            sortBy,
            spacer2,
            searchField
        );

        // Create a title label for the task list
        Label titleLabel = new Label("Tasks");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        VBox taskListPanel = new VBox(10);
        taskListPanel.setPadding(new Insets(10));
        taskListPanel.setStyle("-fx-background-color: white;");
        taskListPanel.getChildren().addAll(
            titleLabel,
            taskToolbar,
            taskList
        );

        // Make the task list take up remaining space
        VBox.setVgrow(taskList, Priority.ALWAYS);
        
        setCenter(taskListPanel);
    }

    private void setupTaskDetailPanel() {
        taskDetailPanel = new VBox(10);
        taskDetailPanel.setPrefWidth(300);
        taskDetailPanel.setPadding(new Insets(10));
        taskDetailPanel.setBorder(new Border(new BorderStroke(
            Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT
        )));
        
        setRight(taskDetailPanel);
    }

    private void loadWorkspaces() {
        TreeItem<NavigationItem> root = new TreeItem<>(new NavigationItem("Root", "root", NavigationType.ROOT));
        navigationTree.setRoot(root);

        clickUpService.getWorkspaces()
            .thenAccept(workspaces -> {
                for (Workspace workspace : workspaces) {
                    TreeItem<NavigationItem> workspaceItem = new TreeItem<>(
                        new NavigationItem(workspace.getName(), workspace.getId(), NavigationType.WORKSPACE)
                    );
                    root.getChildren().add(workspaceItem);
                    
                    // Load spaces and lists for this workspace
                    loadSpacesAndLists(workspaceItem, workspace.getId());
                }
                // Expand the root to show workspaces
                Platform.runLater(() -> root.setExpanded(true));
            })
            .exceptionally(throwable -> {
                showError("Error loading workspaces", throwable.getMessage());
                return null;
            });
    }

    private void loadSpacesAndLists(TreeItem<NavigationItem> workspaceItem, String workspaceId) {
        clickUpService.getSpacesWithLists(workspaceId)
            .thenAccept(spaces -> {
                Platform.runLater(() -> {
                    for (SpaceWithLists space : spaces) {
                        TreeItem<NavigationItem> spaceItem = new TreeItem<>(
                            new NavigationItem(space.getName(), space.getId(), NavigationType.SPACE)
                        );
                        
                        // Add lists under this space
                        for (TaskList list : space.getLists()) {
                            TreeItem<NavigationItem> listItem = new TreeItem<>(
                                new NavigationItem(list.getName(), list.getId(), NavigationType.LIST)
                            );
                            spaceItem.getChildren().add(listItem);
                        }
                        
                        workspaceItem.getChildren().add(spaceItem);
                    }
                    // Expand the workspace item to show its spaces
                    workspaceItem.setExpanded(true);
                });
            })
            .exceptionally(throwable -> {
                showError("Error loading spaces and lists", throwable.getMessage());
                return null;
            });
    }

    private void loadTasksForList(String listId) {
        System.out.println("UI: Loading tasks for list ID: " + listId);
        taskList.getItems().clear();
        
        // Show loading indicator
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(30, 30);
        VBox loadingBox = new VBox(10, loadingIndicator, new Label("Loading tasks..."));
        loadingBox.setAlignment(javafx.geometry.Pos.CENTER);
        taskList.setPlaceholder(loadingBox);
        
        clickUpService.getTasksForList(listId)
            .thenAccept(tasks -> {
                Platform.runLater(() -> {
                    System.out.println("UI: Received " + tasks.size() + " tasks");
                    originalTasks = tasks;
                    filterAndSortTasks();
                    if (tasks.isEmpty()) {
                        Label emptyLabel = new Label("No tasks in this list");
                        emptyLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 14px;");
                        taskList.setPlaceholder(emptyLabel);
                    }
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    String errorMessage = throwable.getCause() != null ? 
                        throwable.getCause().getMessage() : 
                        throwable.getMessage();
                    System.err.println("UI: Error loading tasks: " + errorMessage);
                    
                    // Create error display
                    VBox errorBox = new VBox(10);
                    errorBox.setAlignment(javafx.geometry.Pos.CENTER);
                    Label errorLabel = new Label("Error loading tasks");
                    errorLabel.setStyle("-fx-text-fill: #f44336; -fx-font-size: 14px; -fx-font-weight: bold;");
                    Label detailsLabel = new Label(errorMessage);
                    detailsLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");
                    detailsLabel.setWrapText(true);
                    Button retryButton = new Button("Retry");
                    retryButton.setOnAction(e -> loadTasksForList(listId));
                    
                    errorBox.getChildren().addAll(errorLabel, detailsLabel, retryButton);
                    taskList.setPlaceholder(errorBox);
                    
                    showError("Error Loading Tasks", errorMessage);
                });
                return null;
            });
    }

    private void updateTaskDetailPanel() {
        if (selectedTask == null) {
            taskDetailPanel.getChildren().clear();
            Label noTaskLabel = new Label("No task selected");
            noTaskLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
            taskDetailPanel.getChildren().add(noTaskLabel);
            return;
        }

        taskDetailPanel.getChildren().clear();

        // Task name
        Label nameLabel = new Label("Task Name:");
        TextField nameField = new TextField(selectedTask.getName());
        nameField.setMaxWidth(300);

        // Description
        Label descriptionLabel = new Label("Description:");
        TextArea descriptionArea = new TextArea(selectedTask.getDescription());
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setMaxWidth(300);

        // Status
        Label statusLabel = new Label("Status:");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Open", "In Progress", "Review", "Completed");
        Status currentStatus = selectedTask.getStatus();
        statusCombo.setValue(currentStatus != null ? currentStatus.getStatus() : "Open");

        // Due date
        Label dueDateLabel = new Label("Due Date:");
        DatePicker dueDatePicker = new DatePicker();
        if (selectedTask.getDueDate() != null) {
            dueDatePicker.setValue(selectedTask.getDueDate()
                .atZone(java.time.ZoneOffset.UTC)
                .toLocalDate());
        }

        // Quick status buttons
        HBox quickStatusBox = new HBox(10);
        Button startButton = new Button("Start");
        startButton.setOnAction(e -> {
            Status newStatus = new Status();
            newStatus.setStatus("In Progress");
            selectedTask.setStatus(newStatus);
            saveTaskChanges(selectedTask, nameField, descriptionArea, statusCombo, dueDatePicker);
        });

        Button completeButton = new Button("Complete");
        completeButton.setOnAction(e -> {
            Status newStatus = new Status();
            newStatus.setStatus("Completed");
            selectedTask.setStatus(newStatus);
            saveTaskChanges(selectedTask, nameField, descriptionArea, statusCombo, dueDatePicker);
        });

        quickStatusBox.getChildren().addAll(startButton, completeButton);

        // Save and Delete buttons
        HBox buttonBox = new HBox(10);
        Button saveButton = new Button("Save Changes");
        saveButton.setOnAction(e -> saveTaskChanges(selectedTask, nameField, descriptionArea, statusCombo, dueDatePicker));

        Button deleteButton = new Button("Delete Task");
        deleteButton.setOnAction(e -> deleteTask(selectedTask));

        buttonBox.getChildren().addAll(saveButton, deleteButton);

        // Add all components to the panel
        taskDetailPanel.getChildren().addAll(
            nameLabel, nameField,
            descriptionLabel, descriptionArea,
            statusLabel, statusCombo,
            dueDateLabel, dueDatePicker,
            new Separator(),
            quickStatusBox,
            new Separator(),
            buttonBox
        );

        VBox.setMargin(buttonBox, new Insets(10, 0, 0, 0));
    }

    private void saveTaskChanges(Task task, TextField nameField, TextArea descriptionArea, 
                               ComboBox<String> statusCombo, DatePicker dueDatePicker) {
        task.setName(nameField.getText());
        task.setDescription(descriptionArea.getText());
        
        Status newStatus = new Status();
        newStatus.setStatus(statusCombo.getValue());
        task.setStatus(newStatus);

        if (dueDatePicker.getValue() != null) {
            task.setDueDate(dueDatePicker.getValue()
                .atStartOfDay()
                .toInstant(java.time.ZoneOffset.UTC));
        } else {
            task.setDueDate(null);
        }

        // Show loading indicator
        ProgressIndicator progress = new ProgressIndicator();
        taskDetailPanel.getChildren().add(progress);

        clickUpService.updateTask(task.getId(), task)
            .thenAccept(updatedTask -> {
                Platform.runLater(() -> {
                    taskDetailPanel.getChildren().remove(progress);
                    showSuccess("Task Updated", "Task has been successfully updated.");
                    refreshCurrentView();
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    taskDetailPanel.getChildren().remove(progress);
                    showError("Error", "Failed to update task: " + throwable.getMessage());
                });
                return null;
            });
    }

    private void deleteTask(Task task) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Task");
        alert.setContentText("Are you sure you want to delete this task?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Show loading indicator
            taskDetailPanel.setDisable(true);
            ProgressIndicator progress = new ProgressIndicator();
            progress.setMaxSize(30, 30);
            taskDetailPanel.getChildren().add(progress);
            
            clickUpService.deleteTask(task.getId())
                .thenRun(() -> {
                    Platform.runLater(() -> {
                        taskList.getItems().remove(task);
                        originalTasks.remove(task);
                        selectedTask = null;
                        updateTaskDetailPanel();
                        showSuccess("Task Deleted", "Task has been successfully deleted.");
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showError("Error deleting task", throwable.getMessage());
                        taskDetailPanel.setDisable(false);
                    });
                    return null;
                });
        }
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
        // Auto close after 2 seconds
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(alert::close);
            } catch (InterruptedException e) {
                // Ignore
            }
        }).start();
    }

    private void createNewTask() {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Create New Task");
        dialog.setHeaderText("Enter task details");

        // Create the custom dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Task name");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Task description");
        descriptionArea.setPrefRowCount(3);
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Open", "In Progress", "Review", "Completed");
        statusCombo.setValue("Open");
        DatePicker dueDatePicker = new DatePicker();

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        grid.add(new Label("Status:"), 0, 2);
        grid.add(statusCombo, 1, 2);
        grid.add(new Label("Due Date:"), 0, 3);
        grid.add(dueDatePicker, 1, 3);

        dialog.getDialogPane().setContent(grid);

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                Task newTask = new Task();
                newTask.setName(nameField.getText());
                newTask.setDescription(descriptionArea.getText());
                
                Status newStatus = new Status();
                newStatus.setStatus(statusCombo.getValue());
                newTask.setStatus(newStatus);
                
                if (dueDatePicker.getValue() != null) {
                    newTask.setDueDate(dueDatePicker.getValue()
                        .atStartOfDay()
                        .toInstant(java.time.ZoneOffset.UTC));
                }
                return newTask;
            }
            return null;
        });

        Optional<Task> result = dialog.showAndWait();
        result.ifPresent(task -> {
            TreeItem<NavigationItem> selectedItem = navigationTree.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getValue().getType() == NavigationType.LIST) {
                String listId = selectedItem.getValue().getId();
                
                // Show loading indicator
                ProgressIndicator progress = new ProgressIndicator();
                taskDetailPanel.getChildren().add(progress);

                clickUpService.createTask(listId, task)
                    .thenAccept(createdTask -> {
                        Platform.runLater(() -> {
                            taskDetailPanel.getChildren().remove(progress);
                            showSuccess("Task Created", "New task has been created successfully.");
                            refreshCurrentView();
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            taskDetailPanel.getChildren().remove(progress);
                            showError("Error", "Failed to create task: " + throwable.getMessage());
                        });
                        return null;
                    });
            }
        });
    }

    private void editTask(Task task) {
        // TODO: Implement task editing
    }

    private void refreshCurrentView() {
        TreeItem<NavigationItem> selectedItem = navigationTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getValue().getType() == NavigationType.LIST) {
            loadTasksForList(selectedItem.getValue().getId());
        }
    }

    private void filterAndSortTasks() {
        if (originalTasks == null) return;

        List<Task> filteredTasks = new ArrayList<>(originalTasks);

        // Apply status filter
        String selectedStatus = statusFilter.getValue();
        if (selectedStatus != null && !selectedStatus.equals("All")) {
            filteredTasks.removeIf(task -> {
                Status status = task.getStatus();
                return status == null || !selectedStatus.equals(status.getStatus());
            });
        }

        // Apply search filter
        String searchText = searchField.getText().toLowerCase();
        if (!searchText.isEmpty()) {
            filteredTasks.removeIf(task ->
                (task.getName() == null || !task.getName().toLowerCase().contains(searchText)) &&
                (task.getDescription() == null || !task.getDescription().toLowerCase().contains(searchText))
            );
        }

        // Apply sorting
        String sortCriteria = sortBy.getValue();
        if (sortCriteria != null) {
            switch (sortCriteria) {
                case "Name":
                    filteredTasks.sort(Comparator.comparing(Task::getName,
                        Comparator.nullsLast(String::compareToIgnoreCase)));
                    break;
                case "Status":
                    filteredTasks.sort(Comparator.comparing(
                        task -> task.getStatus() != null ? task.getStatus().getStatus() : "",
                        Comparator.nullsLast(String::compareToIgnoreCase)));
                    break;
                case "Due Date":
                    filteredTasks.sort(Comparator.comparing(Task::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                    break;
            }
        }

        taskList.getItems().setAll(filteredTasks);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class NavigationItem {
        private final String name;
        private final String id;
        private final NavigationType type;

        public NavigationItem(String name, String id, NavigationType type) {
            this.name = name;
            this.id = id;
            this.type = type;
        }

        public String getName() { return name; }
        public String getId() { return id; }
        public NavigationType getType() { return type; }
    }

    private enum NavigationType {
        ROOT, WORKSPACE, SPACE, LIST
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 