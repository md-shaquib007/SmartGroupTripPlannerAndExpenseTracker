package com.tripsync.service;

import com.tripsync.dao.ActivityDao;
import com.tripsync.dao.ExpenseDao;
import com.tripsync.dao.TripMemberDao;
import com.tripsync.model.Expense;
import com.tripsync.model.TripMember;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ExpenseService {

    private final ExpenseDao expenseDao = new ExpenseDao();
    private final TripMemberDao memberDao = new TripMemberDao();
    private final ActivityDao activityDao = new ActivityDao();
    private final TripService tripService = new TripService();

    public Expense addExpense(Expense expense, List<Long> splitAmongUserIds, long createdBy) throws SQLException {
        tripService.requireMember(expense.getTripId(), createdBy);
        if (tripService.isReadOnly(expense.getTripId())) {
            throw new IllegalStateException("Trip is read-only");
        }

        long expenseId = expenseDao.create(expense);
        expense.setId(expenseId);

        List<Long> splitUsers = splitAmongUserIds != null && !splitAmongUserIds.isEmpty()
                ? splitAmongUserIds
                : memberDao.findByTripId(expense.getTripId()).stream().map(TripMember::getUserId).toList();

        BigDecimal share = expense.getAmount().divide(
                BigDecimal.valueOf(splitUsers.size()), 2, RoundingMode.HALF_UP);

        for (Long userId : splitUsers) {
            expenseDao.addSplit(expenseId, userId, share);
        }

        activityDao.log(expense.getTripId(), createdBy, "EXPENSE_ADDED",
                "Expense \"" + expense.getTitle() + "\" added: " + expense.getAmount());
        notifyMembers(expense.getTripId(), createdBy,
                "Expense added: " + expense.getTitle() + " (" + expense.getAmount() + ")");

        tripService.checkBudgetWarning(expense.getTripId());
        return expenseDao.findById(expenseId);
    }

    public List<Expense> getExpenses(long tripId, long userId, String category, Long memberId,
                                     java.time.LocalDate from, java.time.LocalDate to) throws SQLException {
        tripService.requireMember(tripId, userId);
        return expenseDao.findByTripId(tripId, category, memberId, from, to);
    }

    private void notifyMembers(long tripId, long excludeUserId, String message) throws SQLException {
        for (TripMember member : memberDao.findByTripId(tripId)) {
            if (member.getUserId() == excludeUserId) continue;
            activityDao.createNotification(member.getUserId(), tripId, "EXPENSE_ADDED", message);
        }
    }
}
