package com.tripsync.dao;

import com.tripsync.model.EmergencyInfo;
import com.tripsync.util.JdbcUtil;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EmergencyDao {

    public EmergencyInfo findByTripId(long tripId) throws SQLException {
        return JdbcUtil.queryOne(
                "SELECT * FROM emergency_information WHERE trip_id = ?",
                this::map,
                tripId
        );
    }

    public void upsert(EmergencyInfo info) throws SQLException {
        EmergencyInfo existing = findByTripId(info.getTripId());
        if (existing == null) {
            JdbcUtil.insert(
                    """
                    INSERT INTO emergency_information (trip_id, hotel_details, vehicle_details,
                        driver_contact, leader_contact, emergency_numbers, live_location_url, medical_info)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    info.getTripId(), info.getHotelDetails(), info.getVehicleDetails(),
                    info.getDriverContact(), info.getLeaderContact(), info.getEmergencyNumbers(),
                    info.getLiveLocationUrl(), info.getMedicalInfo()
            );
        } else {
            JdbcUtil.update(
                    """
                    UPDATE emergency_information SET hotel_details = ?, vehicle_details = ?,
                        driver_contact = ?, leader_contact = ?, emergency_numbers = ?,
                        live_location_url = ?, medical_info = ? WHERE trip_id = ?
                    """,
                    info.getHotelDetails(), info.getVehicleDetails(), info.getDriverContact(),
                    info.getLeaderContact(), info.getEmergencyNumbers(), info.getLiveLocationUrl(),
                    info.getMedicalInfo(), info.getTripId()
            );
        }
    }

    private EmergencyInfo map(ResultSet rs) throws SQLException {
        EmergencyInfo info = new EmergencyInfo();
        info.setId(rs.getLong("id"));
        info.setTripId(rs.getLong("trip_id"));
        info.setHotelDetails(rs.getString("hotel_details"));
        info.setVehicleDetails(rs.getString("vehicle_details"));
        info.setDriverContact(rs.getString("driver_contact"));
        info.setLeaderContact(rs.getString("leader_contact"));
        info.setEmergencyNumbers(rs.getString("emergency_numbers"));
        info.setLiveLocationUrl(rs.getString("live_location_url"));
        info.setMedicalInfo(rs.getString("medical_info"));
        return info;
    }
}
