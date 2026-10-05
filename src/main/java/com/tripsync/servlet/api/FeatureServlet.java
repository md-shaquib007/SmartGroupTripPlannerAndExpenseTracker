package com.tripsync.servlet.api;

import com.google.gson.JsonObject;
import com.tripsync.dao.ActivityDao;
import com.tripsync.dao.ChecklistDao;
import com.tripsync.dao.EmergencyDao;
import com.tripsync.dao.ExpenseDao;
import com.tripsync.dao.ItineraryDao;
import com.tripsync.dao.MemoryDao;
import com.tripsync.dao.TripDao;
import com.tripsync.filter.AuthFilter;
import com.tripsync.model.ChecklistItem;
import com.tripsync.model.EmergencyInfo;
import com.tripsync.model.Itinerary;
import com.tripsync.model.ItineraryItem;
import com.tripsync.model.Memory;
import com.tripsync.model.User;
import com.tripsync.service.TripService;
import com.tripsync.servlet.BaseServlet;
import com.tripsync.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {
        "/api/itinerary/*",
        "/api/checklist/*",
        "/api/memories/*",
        "/api/analytics/*",
        "/api/notifications/*",
        "/api/emergency/*"
})
public class FeatureServlet extends BaseServlet {

    private final ItineraryDao itineraryDao = new ItineraryDao();
    private final ChecklistDao checklistDao = new ChecklistDao();
    private final MemoryDao memoryDao = new MemoryDao();
    private final ExpenseDao expenseDao = new ExpenseDao();
    private final TripDao tripDao = new TripDao();
    private final ActivityDao activityDao = new ActivityDao();
    private final EmergencyDao emergencyDao = new EmergencyDao();
    private final TripService tripService = new TripService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = AuthFilter.currentUser(req);
        String servletPath = req.getServletPath();
        try {
            long tripId = Long.parseLong(req.getParameter("tripId"));

            if (servletPath.startsWith("/api/itinerary")) {
                tripService.requireMember(tripId, user.getId());
                JsonUtil.writeJson(resp, 200, itineraryDao.findByTripId(tripId));
            } else if (servletPath.startsWith("/api/checklist")) {
                tripService.requireMember(tripId, user.getId());
                JsonUtil.writeJson(resp, 200, checklistDao.findByTripId(tripId));
            } else if (servletPath.startsWith("/api/memories")) {
                tripService.requireMember(tripId, user.getId());
                JsonUtil.writeJson(resp, 200, memoryDao.findByTripId(tripId, user.getId()));
            } else if (servletPath.startsWith("/api/analytics")) {
                tripService.requireMember(tripId, user.getId());
                JsonUtil.writeJson(resp, 200, buildAnalytics(tripId));
            } else if (servletPath.startsWith("/api/notifications")) {
                JsonUtil.writeJson(resp, 200, activityDao.findUnreadByUser(user.getId()));
            } else if (servletPath.startsWith("/api/emergency")) {
                tripService.requireMember(tripId, user.getId());
                JsonUtil.writeJson(resp, 200, emergencyDao.findByTripId(tripId));
            } else {
                JsonUtil.writeError(resp, 404, "Not found");
            }
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = AuthFilter.currentUser(req);
        String servletPath = req.getServletPath();
        try {
            JsonObject body = JsonUtil.parseBody(readBody(req));
            long tripId = JsonUtil.getLong(body, "tripId");

            if (servletPath.startsWith("/api/itinerary")) {
                tripService.requireLeader(tripId, user.getId());
                Itinerary day = new Itinerary();
                day.setTripId(tripId);
                day.setDayNumber(JsonUtil.getInt(body, "dayNumber"));
                day.setTitle(JsonUtil.getString(body, "title"));
                if (JsonUtil.getString(body, "itineraryDate") != null) {
                    day.setItineraryDate(LocalDate.parse(JsonUtil.getString(body, "itineraryDate")));
                }
                long dayId = itineraryDao.createDay(day);

                if (JsonUtil.getString(body, "itemTitle") != null) {
                    ItineraryItem item = new ItineraryItem();
                    item.setItineraryId(dayId);
                    item.setTitle(JsonUtil.getString(body, "itemTitle"));
                    item.setDescription(JsonUtil.getString(body, "description"));
                    item.setLocation(JsonUtil.getString(body, "location"));
                    item.setStatus(ItineraryItem.ItemStatus.APPROVED);
                    itineraryDao.addItem(item);
                }
                activityDao.log(tripId, user.getId(), "ITINERARY_UPDATED", "Itinerary was updated");
                JsonUtil.writeJson(resp, 201, Map.of("id", dayId));
            } else if (servletPath.startsWith("/api/checklist")) {
                tripService.requireMember(tripId, user.getId());
                ChecklistItem item = new ChecklistItem();
                item.setTripId(tripId);
                item.setItemName(JsonUtil.getString(body, "itemName"));
                item.setCategory(JsonUtil.getString(body, "category"));
                item.setCreatedBy(user.getId());
                long id = checklistDao.create(item);
                JsonUtil.writeJson(resp, 201, Map.of("id", id));
            } else if (servletPath.startsWith("/api/memories")) {
                tripService.requireMember(tripId, user.getId());
                Memory memory = new Memory();
                memory.setTripId(tripId);
                memory.setUserId(user.getId());
                memory.setCaption(JsonUtil.getString(body, "caption"));
                memory.setLocation(JsonUtil.getString(body, "location"));
                if (JsonUtil.getString(body, "memoryDate") != null) {
                    memory.setMemoryDate(LocalDate.parse(JsonUtil.getString(body, "memoryDate")));
                }
                long id = memoryDao.create(memory);
                activityDao.log(tripId, user.getId(), "MEMORY_UPLOADED", "New memory uploaded");
                JsonUtil.writeJson(resp, 201, Map.of("id", id));
            } else if (servletPath.startsWith("/api/notifications")) {
                activityDao.markAllRead(user.getId());
                JsonUtil.writeJson(resp, 200, Map.of("message", "All notifications marked as read"));
            } else if (servletPath.startsWith("/api/emergency")) {
                tripService.requireLeader(tripId, user.getId());
                EmergencyInfo info = new EmergencyInfo();
                info.setTripId(tripId);
                info.setHotelDetails(JsonUtil.getString(body, "hotelDetails"));
                info.setVehicleDetails(JsonUtil.getString(body, "vehicleDetails"));
                info.setDriverContact(JsonUtil.getString(body, "driverContact"));
                info.setLeaderContact(JsonUtil.getString(body, "leaderContact"));
                info.setEmergencyNumbers(JsonUtil.getString(body, "emergencyNumbers"));
                info.setLiveLocationUrl(JsonUtil.getString(body, "liveLocationUrl"));
                info.setMedicalInfo(JsonUtil.getString(body, "medicalInfo"));
                emergencyDao.upsert(info);
                JsonUtil.writeJson(resp, 200, Map.of("message", "Emergency info saved"));
            } else {
                JsonUtil.writeError(resp, 404, "Not found");
            }
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = AuthFilter.currentUser(req);
        String servletPath = req.getServletPath();
        try {
            JsonObject body = JsonUtil.parseBody(readBody(req));

            if (servletPath.startsWith("/api/checklist")) {
                long itemId = JsonUtil.getLong(body, "itemId");
                long tripId = JsonUtil.getLong(body, "tripId");
                tripService.requireMember(tripId, user.getId());
                checklistDao.setAssignment(itemId, user.getId(),
                        ChecklistItem.AssignmentStatus.valueOf(JsonUtil.getString(body, "status")));
                JsonUtil.writeJson(resp, 200, Map.of("message", "Assignment updated"));
            } else if (servletPath.startsWith("/api/itinerary")) {
                long itemId = JsonUtil.getLong(body, "itemId");
                long tripId = JsonUtil.getLong(body, "tripId");
                tripService.requireLeader(tripId, user.getId());
                itineraryDao.updateItemStatus(itemId,
                        ItineraryItem.ItemStatus.valueOf(JsonUtil.getString(body, "status")));
                JsonUtil.writeJson(resp, 200, Map.of("message", "Item status updated"));
            } else if (servletPath.startsWith("/api/memories")) {
                long memoryId = JsonUtil.getLong(body, "memoryId");
                long tripId = JsonUtil.getLong(body, "tripId");
                tripService.requireMember(tripId, user.getId());
                memoryDao.toggleLike(memoryId, user.getId());
                JsonUtil.writeJson(resp, 200, Map.of("message", "Like toggled"));
            } else {
                JsonUtil.writeError(resp, 404, "Not found");
            }
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    private Map<String, Object> buildAnalytics(long tripId) throws Exception {
        var trip = tripDao.findById(tripId);
        var totalSpent = expenseDao.getTotalSpent(tripId);
        var byCategory = expenseDao.getSpendingByCategory(tripId);
        var balances = expenseDao.getMemberBalances(tripId);

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("budget", trip.getBudget());
        analytics.put("totalSpent", totalSpent);
        analytics.put("remaining", trip.getBudget().subtract(totalSpent));
        analytics.put("spentPercentage", trip.getBudget().compareTo(java.math.BigDecimal.ZERO) > 0
                ? totalSpent.multiply(java.math.BigDecimal.valueOf(100)).divide(trip.getBudget(), 2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO);
        analytics.put("byCategory", byCategory);
        analytics.put("memberBalances", balances);
        analytics.put("budgetWarning", trip.getBudget().compareTo(java.math.BigDecimal.ZERO) > 0
                && totalSpent.divide(trip.getBudget(), 4, java.math.RoundingMode.HALF_UP).doubleValue() >= 0.8);
        return analytics;
    }
}
