package com.tripsync.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ChecklistItem {
    private Long id;
    private Long tripId;
    private String itemName;
    private String category;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Map<Long, AssignmentStatus> assignments = new HashMap<>();

    public enum AssignmentStatus { BRINGING, NOT_BRINGING }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Map<Long, AssignmentStatus> getAssignments() { return assignments; }
    public void setAssignments(Map<Long, AssignmentStatus> assignments) { this.assignments = assignments; }
}
