package com.tripsync.servlet.api;

import com.google.gson.JsonObject;
import com.tripsync.dao.ActivityDao;
import com.tripsync.dao.TripDao;
import com.tripsync.dao.TripMemberDao;
import com.tripsync.filter.AuthFilter;
import com.tripsync.model.Trip;
import com.tripsync.model.TripMember;
import com.tripsync.model.User;
import com.tripsync.service.TripService;
import com.tripsync.servlet.BaseServlet;
import com.tripsync.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@WebServlet("/api/trips/*")
public class TripServlet extends BaseServlet {

    private final TripDao tripDao = new TripDao();
    private final TripMemberDao memberDao = new TripMemberDao();
    private final TripService tripService = new TripService();
    private final ActivityDao activityDao = new ActivityDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = AuthFilter.currentUser(req);
        try {
            String path = req.getPathInfo();
            if (path == null || "/".equals(path)) {
                String q = req.getParameter("q");
                String status = req.getParameter("status");
                List<Trip> trips = q != null || status != null
                        ? tripDao.search(q, status)
                        : tripService.getUserTrips(user.getId());
                JsonUtil.writeJson(resp, 200, trips);
                return;
            }

            String[] parts = path.split("/");
            long tripId = Long.parseLong(parts[1]);

            if (parts.length == 2) {
                tripService.requireMember(tripId, user.getId());
                JsonUtil.writeJson(resp, 200, tripDao.findById(tripId));
            } else if (parts.length == 3 && "members".equals(parts[2])) {
                tripService.requireMember(tripId, user.getId());
                JsonUtil.writeJson(resp, 200, memberDao.findByTripId(tripId));
            } else if (parts.length == 3 && "timeline".equals(parts[2])) {
                tripService.requireMember(tripId, user.getId());
                JsonUtil.writeJson(resp, 200, new ActivityDao().findByTripId(tripId));
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
        try {
            String path = req.getPathInfo();
            JsonObject body = JsonUtil.parseBody(readBody(req));

            if (path == null || "/".equals(path)) {
                Trip trip = buildTrip(body);
                Trip created = tripService.createTrip(trip, user.getId());
                JsonUtil.writeJson(resp, 201, created);
                return;
            }

            String[] parts = path.split("/");
            long tripId = Long.parseLong(parts[1]);

            if (parts.length == 3 && "join".equals(parts[2])) {
                String code = JsonUtil.getString(body, "inviteCode");
                if (code == null) code = req.getParameter("code");
                Trip joined = tripService.joinTrip(code, user.getId());
                JsonUtil.writeJson(resp, 200, joined);
            } else if (parts.length == 4 && "members".equals(parts[2])) {
                long targetId = Long.parseLong(parts[3]);
                String action = JsonUtil.getString(body, "action");
                if ("promote".equals(action)) {
                    tripService.promoteMember(tripId, targetId,
                            TripMember.MemberRole.valueOf(JsonUtil.getString(body, "role")), user.getId());
                } else if ("remove".equals(action)) {
                    tripService.removeMember(tripId, targetId, user.getId());
                } else if ("transfer".equals(action)) {
                    tripService.transferOwnership(tripId, targetId, user.getId());
                }
                JsonUtil.writeJson(resp, 200, Map.of("message", "Success"));
            } else if (parts.length == 3 && "archive".equals(parts[2])) {
                tripService.archiveTrip(tripId, user.getId());
                JsonUtil.writeJson(resp, 200, Map.of("message", "Trip archived"));
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
        try {
            String[] parts = req.getPathInfo().split("/");
            long tripId = Long.parseLong(parts[1]);
            tripService.requireLeader(tripId, user.getId());

            Trip trip = tripDao.findById(tripId);
            JsonObject body = JsonUtil.parseBody(readBody(req));
            if (JsonUtil.getString(body, "name") != null) trip.setName(JsonUtil.getString(body, "name"));
            if (JsonUtil.getString(body, "destination") != null) trip.setDestination(JsonUtil.getString(body, "destination"));
            if (JsonUtil.getString(body, "startDate") != null) trip.setStartDate(LocalDate.parse(JsonUtil.getString(body, "startDate")));
            if (JsonUtil.getString(body, "endDate") != null) trip.setEndDate(LocalDate.parse(JsonUtil.getString(body, "endDate")));
            if (JsonUtil.getDouble(body, "budget") != null) trip.setBudget(BigDecimal.valueOf(JsonUtil.getDouble(body, "budget")));
            if (JsonUtil.getString(body, "description") != null) trip.setDescription(JsonUtil.getString(body, "description"));
            if (JsonUtil.getString(body, "status") != null) trip.setStatus(Trip.TripStatus.valueOf(JsonUtil.getString(body, "status")));

            tripDao.update(trip);
            activityDao.log(tripId, user.getId(), "TRIP_UPDATED", "Trip details were updated");
            JsonUtil.writeJson(resp, 200, tripDao.findById(tripId));
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = AuthFilter.currentUser(req);
        try {
            long tripId = Long.parseLong(req.getPathInfo().split("/")[1]);
            tripService.requireLeader(tripId, user.getId());
            tripDao.delete(tripId);
            JsonUtil.writeJson(resp, 200, Map.of("message", "Trip deleted"));
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    private Trip buildTrip(JsonObject body) {
        Trip trip = new Trip();
        trip.setName(JsonUtil.getString(body, "name"));
        trip.setDestination(JsonUtil.getString(body, "destination"));
        trip.setStartDate(LocalDate.parse(JsonUtil.getString(body, "startDate")));
        trip.setEndDate(LocalDate.parse(JsonUtil.getString(body, "endDate")));
        trip.setBudget(BigDecimal.valueOf(JsonUtil.getDouble(body, "budget")));
        trip.setMaxMembers(JsonUtil.getInt(body, "maxMembers") != null ? JsonUtil.getInt(body, "maxMembers") : 20);
        trip.setDescription(JsonUtil.getString(body, "description"));
        trip.setCurrency(JsonUtil.getString(body, "currency") != null ? JsonUtil.getString(body, "currency") : "INR");
        return trip;
    }
}
