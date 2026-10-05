package com.tripsync.model;

import java.time.LocalTime;

public class ItineraryItem {
    private Long id;
    private Long itineraryId;
    private String title;
    private String description;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private int sortOrder;
    private ItemStatus status;
    private Long suggestedBy;

    public enum ItemStatus { APPROVED, SUGGESTED, REJECTED }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }
    public Long getSuggestedBy() { return suggestedBy; }
    public void setSuggestedBy(Long suggestedBy) { this.suggestedBy = suggestedBy; }
}
