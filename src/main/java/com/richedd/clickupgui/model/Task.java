package com.richedd.clickupgui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {
    private String id;
    private String name;
    private String description;
    private String status;
    
    @JsonProperty("date_created")
    private Instant dateCreated;
    
    @JsonProperty("date_updated")
    private Instant dateUpdated;
    
    @JsonProperty("date_closed")
    private Instant dateClosed;

    // Default constructor for Jackson
    public Task() {}

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Instant dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Instant getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(Instant dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public Instant getDateClosed() {
        return dateClosed;
    }

    public void setDateClosed(Instant dateClosed) {
        this.dateClosed = dateClosed;
    }
} 