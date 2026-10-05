package com.tripsync.service;

import com.tripsync.dao.ActivityDao;
import com.tripsync.dao.ExpenseDao;
import com.tripsync.dao.TripDao;
import com.tripsync.dao.TripMemberDao;
import com.tripsync.model.Trip;
import com.tripsync.model.TripMember;
import com.tripsync.util.InviteCodeUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class TripService {

    private final TripDao tripDao = new TripDao();
    private final TripMemberDao memberDao = new TripMemberDao();
    private final ActivityDao activityDao = new ActivityDao();
    private final ExpenseDao expenseDao = new ExpenseDao();

    public Trip createTrip(Trip trip, long leaderId) throws SQLException {
        trip.setLeaderId(leaderId);
        trip.setInviteCode(InviteCodeUtil.generate(8));
        trip.setStatus(Trip.TripStatus.PLANNING);
        long tripId = tripDao.create(trip);
        trip.setId(tripId);
        memberDao.addMember(tripId, leaderId, TripMember.MemberRole.LEADER);
        activityDao.log(tripId, leaderId, "TRIP_CREATED", "Trip \"" + trip.getName() + "\" was created");
        return tripDao.findById(tripId);
    }

    public Trip joinTrip(String inviteCode, long userId) throws SQLException {
        Trip trip = tripDao.findByInviteCode(inviteCode);
        if (trip == null) {
            throw new IllegalArgumentException("Invalid invite code");
        }
        if (trip.getStatus() == Trip.TripStatus.ARCHIVED || trip.getStatus() == Trip.TripStatus.COMPLETED) {
            throw new IllegalArgumentException("This trip is no longer accepting members");
        }
        if (memberDao.isMember(trip.getId(), userId)) {
            throw new IllegalArgumentException("You are already a member of this trip");
        }
        if (memberDao.countMembers(trip.getId()) >= trip.getMaxMembers()) {
            throw new IllegalArgumentException("Trip has reached maximum member limit");
        }
        memberDao.addMember(trip.getId(), userId, TripMember.MemberRole.MEMBER);
        activityDao.log(trip.getId(), userId, "MEMBER_JOINED", "A new member joined the trip");
        notifyAllMembers(trip.getId(), userId, "MEMBER_JOINED", "A new member joined \"" + trip.getName() + "\"");
        return tripDao.findById(trip.getId());
    }

    public void promoteMember(long tripId, long targetUserId, TripMember.MemberRole newRole, long actorId) throws SQLException {
        requireLeader(tripId, actorId);
        memberDao.updateRole(tripId, targetUserId, newRole);
        activityDao.log(tripId, actorId, "ROLE_UPDATED", "Member role updated to " + newRole.name());
    }

    public void removeMember(long tripId, long targetUserId, long actorId) throws SQLException {
        requireLeader(tripId, actorId);
        if (targetUserId == actorId) {
            throw new IllegalArgumentException("Leader cannot remove themselves. Transfer ownership first.");
        }
        memberDao.removeMember(tripId, targetUserId);
        activityDao.log(tripId, actorId, "MEMBER_REMOVED", "A member was removed from the trip");
    }

    public void transferOwnership(long tripId, long newLeaderId, long currentLeaderId) throws SQLException {
        requireLeader(tripId, currentLeaderId);
        if (!memberDao.isMember(tripId, newLeaderId)) {
            throw new IllegalArgumentException("New leader must be a trip member");
        }
        tripDao.updateLeader(tripId, newLeaderId);
        memberDao.updateRole(tripId, newLeaderId, TripMember.MemberRole.LEADER);
        memberDao.updateRole(tripId, currentLeaderId, TripMember.MemberRole.CO_LEADER);
        activityDao.log(tripId, currentLeaderId, "OWNERSHIP_TRANSFERRED", "Trip ownership was transferred");
    }

    public void archiveTrip(long tripId, long userId) throws SQLException {
        requireLeader(tripId, userId);
        Trip trip = tripDao.findById(tripId);
        trip.setStatus(Trip.TripStatus.ARCHIVED);
        tripDao.update(trip);
        activityDao.log(tripId, userId, "TRIP_ARCHIVED", "Trip was archived");
    }

    public void checkBudgetWarning(long tripId) throws SQLException {
        Trip trip = tripDao.findById(tripId);
        if (trip == null || trip.getBudget().compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal spent = expenseDao.getTotalSpent(tripId);
        double ratio = spent.divide(trip.getBudget(), 4, java.math.RoundingMode.HALF_UP).doubleValue();
        if (ratio >= 0.8) {
            notifyAllMembers(tripId, null, "BUDGET_WARNING",
                    String.format("Budget alert: %.0f%% of budget spent on \"%s\"", ratio * 100, trip.getName()));
        }
    }

    public List<Trip> getUserTrips(long userId) throws SQLException {
        return tripDao.findByUserId(userId);
    }

    public void requireMember(long tripId, long userId) throws SQLException {
        if (!memberDao.isMember(tripId, userId)) {
            throw new SecurityException("You are not a member of this trip");
        }
    }

    public void requireLeader(long tripId, long userId) throws SQLException {
        TripMember member = memberDao.findMember(tripId, userId);
        if (member == null || member.getRole() == TripMember.MemberRole.MEMBER) {
            throw new SecurityException("Only leaders can perform this action");
        }
    }

    public boolean isReadOnly(long tripId) throws SQLException {
        Trip trip = tripDao.findById(tripId);
        return trip != null && (trip.getStatus() == Trip.TripStatus.ARCHIVED || trip.getStatus() == Trip.TripStatus.COMPLETED);
    }

    private void notifyAllMembers(long tripId, Long excludeUserId, String type, String message) throws SQLException {
        for (TripMember member : memberDao.findByTripId(tripId)) {
            if (excludeUserId != null && member.getUserId().equals(excludeUserId)) continue;
            activityDao.createNotification(member.getUserId(), tripId, type, message);
        }
    }
}
