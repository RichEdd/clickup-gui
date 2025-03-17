# ClickUp GUI

A Java-based GUI application for interacting with ClickUp.

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Building the Project

To build the project, run:

```bash
mvn clean install
```

## Running the Application

To run the application, use:

```bash
mvn javafx:run
```

## Development

This project uses:
- JavaFX for the GUI
- OkHttp for API communication
- Jackson for JSON processing
- JUnit 5 for testing

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── richedd/
│   │           └── clickupgui/
│   │               └── Main.java
│   └── resources/
└── test/
    ├── java/
    │   └── com/
    │       └── richedd/
    │           └── clickupgui/
    └── resources/
``` 