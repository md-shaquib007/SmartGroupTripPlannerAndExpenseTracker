package com.tripsync.model;

public class EmergencyInfo {
    private Long id;
    private Long tripId;
    private String hotelDetails;
    private String vehicleDetails;
    private String driverContact;
    private String leaderContact;
    private String emergencyNumbers;
    private String liveLocationUrl;
    private String medicalInfo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public String getHotelDetails() { return hotelDetails; }
    public void setHotelDetails(String hotelDetails) { this.hotelDetails = hotelDetails; }
    public String getVehicleDetails() { return vehicleDetails; }
    public void setVehicleDetails(String vehicleDetails) { this.vehicleDetails = vehicleDetails; }
    public String getDriverContact() { return driverContact; }
    public void setDriverContact(String driverContact) { this.driverContact = driverContact; }
    public String getLeaderContact() { return leaderContact; }
    public void setLeaderContact(String leaderContact) { this.leaderContact = leaderContact; }
    public String getEmergencyNumbers() { return emergencyNumbers; }
    public void setEmergencyNumbers(String emergencyNumbers) { this.emergencyNumbers = emergencyNumbers; }
    public String getLiveLocationUrl() { return liveLocationUrl; }
    public void setLiveLocationUrl(String liveLocationUrl) { this.liveLocationUrl = liveLocationUrl; }
    public String getMedicalInfo() { return medicalInfo; }
    public void setMedicalInfo(String medicalInfo) { this.medicalInfo = medicalInfo; }
}
