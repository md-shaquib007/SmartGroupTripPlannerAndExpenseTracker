package com.tripsync.dao;

import com.tripsync.model.Expense;
import com.tripsync.model.ExpenseSplit;
import com.tripsync.util.JdbcUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseDao {

    public Expense findById(long id) throws SQLException {
        Expense expense = JdbcUtil.queryOne(
                """
                SELECT e.*, u.name AS paid_by_name
                FROM expenses e
                JOIN users u ON u.id = e.paid_by
                WHERE e.id = ?
                """,
                this::mapExpense,
                id
        );
        if (expense != null) {
            expense.setSplits(findSplits(id));
        }
        return expense;
    }

    public List<Expense> findByTripId(long tripId, String category, Long memberId,
                                      LocalDate fromDate, LocalDate toDate) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT e.*, u.name AS paid_by_name
                FROM expenses e
                JOIN users u ON u.id = e.paid_by
                WHERE e.trip_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(tripId);

        if (category != null && !category.isBlank()) {
            sql.append(" AND e.category = ?");
            params.add(category);
        }
        if (memberId != null) {
            sql.append(" AND e.paid_by = ?");
            params.add(memberId);
        }
        if (fromDate != null) {
            sql.append(" AND e.expense_date >= ?");
            params.add(Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND e.expense_date <= ?");
            params.add(Date.valueOf(toDate));
        }
        sql.append(" ORDER BY e.expense_date DESC, e.created_at DESC");

        List<Expense> expenses = new ArrayList<>();
        Map<Long, Expense> expenseMap = new HashMap<>();

        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Expense e = mapExpense(rs);
                    e.setSplits(new ArrayList<>());
                    expenses.add(e);
                    expenseMap.put(e.getId(), e);
                }
            }
        }

        // Batch-load all splits in ONE query instead of N individual queries (fixes N+1)
        if (!expenseMap.isEmpty()) {
            String ids = expenseMap.keySet().stream()
                    .map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
            String splitSql = """
                    SELECT es.*, u.name AS user_name
                    FROM expense_splits es
                    JOIN users u ON u.id = es.user_id
                    WHERE es.expense_id IN (""" + ids + ")";
            try (Connection conn = JdbcUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(splitSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ExpenseSplit s = new ExpenseSplit();
                    s.setId(rs.getLong("id"));
                    s.setExpenseId(rs.getLong("expense_id"));
                    s.setUserId(rs.getLong("user_id"));
                    s.setUserName(rs.getString("user_name"));
                    s.setShareAmount(rs.getBigDecimal("share_amount"));
                    Expense parent = expenseMap.get(s.getExpenseId());
                    if (parent != null) parent.getSplits().add(s);
                }
            }
        }
        return expenses;
    }

    public long create(Expense expense) throws SQLException {
        return JdbcUtil.insert(
                """
                INSERT INTO expenses (trip_id, title, amount, category, paid_by, expense_date,
                                      notes, receipt_url, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                expense.getTripId(), expense.getTitle(), expense.getAmount(),
                expense.getCategory().name(), expense.getPaidBy(),
                Date.valueOf(expense.getExpenseDate()), expense.getNotes(),
                expense.getReceiptUrl(), expense.getCreatedBy()
        );
    }

    public void addSplit(long expenseId, long userId, BigDecimal share) throws SQLException {
        JdbcUtil.insert(
                "INSERT INTO expense_splits (expense_id, user_id, share_amount) VALUES (?, ?, ?)",
                expenseId, userId, share
        );
    }

    public BigDecimal getTotalSpent(long tripId) throws SQLException {
        BigDecimal total = JdbcUtil.queryOne(
                "SELECT COALESCE(SUM(amount), 0) AS total FROM expenses WHERE trip_id = ?",
                rs -> rs.getBigDecimal("total"),
                tripId
        );
        return total != null ? total : BigDecimal.ZERO;
    }

    public Map<String, BigDecimal> getSpendingByCategory(long tripId) throws SQLException {
        Map<String, BigDecimal> map = new HashMap<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT category, SUM(amount) AS total FROM expenses WHERE trip_id = ? GROUP BY category")) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("category"), rs.getBigDecimal("total"));
                }
            }
        }
        return map;
    }

    public Map<Long, BigDecimal[]> getMemberBalances(long tripId) throws SQLException {
        Map<Long, BigDecimal[]> balances = new HashMap<>();
        String sql = """
                SELECT tm.user_id,
                       COALESCE(paid.total_paid, 0) AS total_paid,
                       COALESCE(owed.total_owed, 0) AS total_owed
                FROM trip_members tm
                LEFT JOIN (
                    SELECT paid_by, SUM(amount) AS total_paid
                    FROM expenses WHERE trip_id = ? GROUP BY paid_by
                ) paid ON paid.paid_by = tm.user_id
                LEFT JOIN (
                    SELECT es.user_id, SUM(es.share_amount) AS total_owed
                    FROM expense_splits es
                    JOIN expenses e ON e.id = es.expense_id
                    WHERE e.trip_id = ? GROUP BY es.user_id
                ) owed ON owed.user_id = tm.user_id
                WHERE tm.trip_id = ?
                """;
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            ps.setLong(2, tripId);
            ps.setLong(3, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long userId = rs.getLong("user_id");
                    balances.put(userId, new BigDecimal[]{
                            rs.getBigDecimal("total_paid"),
                            rs.getBigDecimal("total_owed")
                    });
                }
            }
        }
        return balances;
    }

    private List<ExpenseSplit> findSplits(long expenseId) throws SQLException {
        List<ExpenseSplit> splits = new ArrayList<>();
        String sql = """
                SELECT es.*, u.name AS user_name
                FROM expense_splits es
                JOIN users u ON u.id = es.user_id
                WHERE es.expense_id = ?
                """;
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, expenseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ExpenseSplit s = new ExpenseSplit();
                    s.setId(rs.getLong("id"));
                    s.setExpenseId(rs.getLong("expense_id"));
                    s.setUserId(rs.getLong("user_id"));
                    s.setUserName(rs.getString("user_name"));
                    s.setShareAmount(rs.getBigDecimal("share_amount"));
                    splits.add(s);
                }
            }
        }
        return splits;
    }

    private Expense mapExpense(ResultSet rs) throws SQLException {
        Expense e = new Expense();
        e.setId(rs.getLong("id"));
        e.setTripId(rs.getLong("trip_id"));
        e.setTitle(rs.getString("title"));
        e.setAmount(rs.getBigDecimal("amount"));
        e.setCategory(Expense.ExpenseCategory.valueOf(rs.getString("category")));
        e.setPaidBy(rs.getLong("paid_by"));
        e.setPaidByName(rs.getString("paid_by_name"));
        Date d = rs.getDate("expense_date");
        if (d != null) e.setExpenseDate(d.toLocalDate());
        e.setNotes(rs.getString("notes"));
        e.setReceiptUrl(rs.getString("receipt_url"));
        e.setCreatedBy(rs.getLong("created_by"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) e.setCreatedAt(created.toLocalDateTime());
        return e;
    }
}
