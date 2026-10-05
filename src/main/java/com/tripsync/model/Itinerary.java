package com.tripsync.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Itinerary {
    private Long id;
    private Long tripId;
    private int dayNumber;
    private String title;
    private LocalDate itineraryDate;
    private List<ItineraryItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public int getDayNumber() { return dayNumber; }
    public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getItineraryDate() { return itineraryDate; }
    public void setItineraryDate(LocalDate itineraryDate) { this.itineraryDate = itineraryDate; }
    public List<ItineraryItem> getItems() { return items; }
    public void setItems(List<ItineraryItem> items) { this.items = items; }
}
