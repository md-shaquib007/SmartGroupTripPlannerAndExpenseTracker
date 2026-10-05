package com.tripsync.service;

import com.tripsync.dao.ExpenseDao;
import com.tripsync.dao.TripMemberDao;
import com.tripsync.model.Settlement;
import com.tripsync.model.TripMember;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates minimum-transaction settlements using a greedy creditor/debtor matching algorithm.
 */
public class SettlementService {

    private final ExpenseDao expenseDao = new ExpenseDao();
    private final TripMemberDao memberDao = new TripMemberDao();

    public List<Settlement> calculateSettlements(long tripId) throws SQLException {
        Map<Long, BigDecimal[]> balances = expenseDao.getMemberBalances(tripId);
        Map<Long, String> names = new HashMap<>();
        for (TripMember member : memberDao.findByTripId(tripId)) {
            if (member != null && member.getUserId() != 0) {
                names.put(member.getUserId(), member.getUserName() != null ? member.getUserName() : "User #" + member.getUserId());
            }
        }
        return calculateSettlementsFromBalances(balances, names);
    }

    public List<Settlement> calculateSettlementsFromBalances(Map<Long, BigDecimal[]> balances, Map<Long, String> names) {
        List<PersonBalance> creditors = new ArrayList<>();
        List<PersonBalance> debtors = new ArrayList<>();

        if (balances == null) {
            return new ArrayList<>();
        }

        for (Map.Entry<Long, BigDecimal[]> entry : balances.entrySet()) {
            if (entry.getKey() == null) continue;
            BigDecimal paid = entry.getValue() != null && entry.getValue().length > 0 && entry.getValue()[0] != null ? entry.getValue()[0] : BigDecimal.ZERO;
            BigDecimal owed = entry.getValue() != null && entry.getValue().length > 1 && entry.getValue()[1] != null ? entry.getValue()[1] : BigDecimal.ZERO;
            BigDecimal net = paid.subtract(owed).setScale(2, RoundingMode.HALF_UP);

            if (net.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new PersonBalance(entry.getKey(), net));
            } else if (net.compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new PersonBalance(entry.getKey(), net.abs()));
            }
        }

        creditors.sort(Comparator.comparing(PersonBalance::amount).reversed());
        debtors.sort(Comparator.comparing(PersonBalance::amount).reversed());

        List<Settlement> settlements = new ArrayList<>();
        int i = 0, j = 0;

        while (i < creditors.size() && j < debtors.size()) {
            PersonBalance creditor = creditors.get(i);
            PersonBalance debtor = debtors.get(j);
            BigDecimal amount = creditor.amount.min(debtor.amount).setScale(2, RoundingMode.HALF_UP);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                i++;
                j++;
                continue;
            }

            String debtorName = names != null ? names.getOrDefault(debtor.userId, "User #" + debtor.userId) : "User #" + debtor.userId;
            String creditorName = names != null ? names.getOrDefault(creditor.userId, "User #" + creditor.userId) : "User #" + creditor.userId;
            settlements.add(new Settlement(
                    debtor.userId, debtorName,
                    creditor.userId, creditorName,
                    amount
            ));

            creditor.amount = creditor.amount.subtract(amount);
            debtor.amount = debtor.amount.subtract(amount);

            if (creditor.amount.compareTo(new BigDecimal("0.001")) <= 0) i++;
            if (debtor.amount.compareTo(new BigDecimal("0.001")) <= 0) j++;
        }

        return settlements;
    }

    private static class PersonBalance {
        final long userId;
        BigDecimal amount;

        PersonBalance(long userId, BigDecimal amount) {
            this.userId = userId;
            this.amount = amount;
        }

        BigDecimal amount() {
            return amount;
        }
    }
}
