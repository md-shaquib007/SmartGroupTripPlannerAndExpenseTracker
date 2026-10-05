package com.tripsync.servlet.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tripsync.filter.AuthFilter;
import com.tripsync.model.Expense;
import com.tripsync.model.User;
import com.tripsync.service.ExpenseService;
import com.tripsync.service.SettlementService;
import com.tripsync.service.TripService;
import com.tripsync.servlet.BaseServlet;
import com.tripsync.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet("/api/expenses/*")
public class ExpenseServlet extends BaseServlet {

    private final ExpenseService expenseService = new ExpenseService();
    private final SettlementService settlementService = new SettlementService();
    private final TripService tripService = new TripService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = AuthFilter.currentUser(req);
        try {
            String path = req.getPathInfo();
            long tripId = Long.parseLong(req.getParameter("tripId"));

            if (path != null && path.endsWith("/settlements")) {
                tripService.requireMember(tripId, user.getId());
                JsonUtil.writeJson(resp, 200, settlementService.calculateSettlements(tripId));
                return;
            }

            String category = req.getParameter("category");
            Long memberId = req.getParameter("memberId") != null ? Long.parseLong(req.getParameter("memberId")) : null;
            LocalDate from = req.getParameter("from") != null ? LocalDate.parse(req.getParameter("from")) : null;
            LocalDate to = req.getParameter("to") != null ? LocalDate.parse(req.getParameter("to")) : null;

            JsonUtil.writeJson(resp, 200, expenseService.getExpenses(tripId, user.getId(), category, memberId, from, to));
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = AuthFilter.currentUser(req);
        try {
            JsonObject body = JsonUtil.parseBody(readBody(req));
            Expense expense = new Expense();
            expense.setTripId(JsonUtil.getLong(body, "tripId"));
            expense.setTitle(JsonUtil.getString(body, "title"));
            expense.setAmount(BigDecimal.valueOf(JsonUtil.getDouble(body, "amount")));
            expense.setCategory(Expense.ExpenseCategory.valueOf(JsonUtil.getString(body, "category")));
            expense.setPaidBy(JsonUtil.getLong(body, "paidBy") != null ? JsonUtil.getLong(body, "paidBy") : user.getId());
            expense.setExpenseDate(LocalDate.parse(JsonUtil.getString(body, "expenseDate")));
            expense.setNotes(JsonUtil.getString(body, "notes"));
            expense.setCreatedBy(user.getId());

            List<Long> splitAmong = new ArrayList<>();
            JsonArray arr = body.getAsJsonArray("splitAmong");
            if (arr != null) {
                arr.forEach(el -> splitAmong.add(el.getAsLong()));
            }

            Expense created = expenseService.addExpense(expense, splitAmong, user.getId());
            JsonUtil.writeJson(resp, 201, created);
        } catch (Exception e) {
            handleError(resp, e);
        }
    }
}
