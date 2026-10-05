package com.tripsync.service;

import com.tripsync.model.Settlement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SettlementServiceTest {

    private SettlementService settlementService;
    private Map<Long, String> names;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService();
        names = new HashMap<>();
        names.put(1L, "Alice");
        names.put(2L, "Bob");
        names.put(3L, "Charlie");
        names.put(4L, "David");
    }

    @Test
    @DisplayName("Should return empty list when all net balances are zero")
    void testZeroBalances() {
        Map<Long, BigDecimal[]> balances = new HashMap<>();
        balances.put(1L, new BigDecimal[]{new BigDecimal("50.00"), new BigDecimal("50.00")});
        balances.put(2L, new BigDecimal[]{new BigDecimal("100.00"), new BigDecimal("100.00")});

        List<Settlement> settlements = settlementService.calculateSettlementsFromBalances(balances, names);
        assertTrue(settlements.isEmpty(), "Settlement list should be empty when no net debt exists");
    }

    @Test
    @DisplayName("Should correctly calculate simple 1-to-1 debt settlement")
    void testSimpleOneToOneSettlement() {
        Map<Long, BigDecimal[]> balances = new HashMap<>();
        // Alice paid 100, owed 0 => net +100
        balances.put(1L, new BigDecimal[]{new BigDecimal("100.00"), BigDecimal.ZERO});
        // Bob paid 0, owed 100 => net -100
        balances.put(2L, new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("100.00")});

        List<Settlement> settlements = settlementService.calculateSettlementsFromBalances(balances, names);
        assertEquals(1, settlements.size());

        Settlement s = settlements.get(0);
        assertEquals(2L, s.getFromUserId());
        assertEquals("Bob", s.getFromUserName());
        assertEquals(1L, s.getToUserId());
        assertEquals("Alice", s.getToUserName());
        assertEquals(new BigDecimal("100.00"), s.getAmount());
    }

    @Test
    @DisplayName("Should minimize transactions for 3-person circular group expenses")
    void testThreePersonGroupSettlement() {
        Map<Long, BigDecimal[]> balances = new HashMap<>();
        // Total expense 300 (100 per person).
        // Alice paid 300, owed 100 => net +200
        balances.put(1L, new BigDecimal[]{new BigDecimal("300.00"), new BigDecimal("100.00")});
        // Bob paid 0, owed 100 => net -100
        balances.put(2L, new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("100.00")});
        // Charlie paid 0, owed 100 => net -100
        balances.put(3L, new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("100.00")});

        List<Settlement> settlements = settlementService.calculateSettlementsFromBalances(balances, names);
        assertEquals(2, settlements.size());

        BigDecimal totalSettled = settlements.stream()
                .map(Settlement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("200.00"), totalSettled);

        for (Settlement s : settlements) {
            assertEquals(1L, s.getToUserId());
        }
    }

    @Test
    @DisplayName("Should handle unequal splits and multi-debtor greedy matching correctly")
    void testMultiPersonGreedyMatching() {
        Map<Long, BigDecimal[]> balances = new HashMap<>();
        // Alice net +150
        balances.put(1L, new BigDecimal[]{new BigDecimal("200.00"), new BigDecimal("50.00")});
        // Bob net +50
        balances.put(2L, new BigDecimal[]{new BigDecimal("100.00"), new BigDecimal("50.00")});
        // Charlie net -120
        balances.put(3L, new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("120.00")});
        // David net -80
        balances.put(4L, new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("80.00")});

        List<Settlement> settlements = settlementService.calculateSettlementsFromBalances(balances, names);
        assertNotNull(settlements);
        assertFalse(settlements.isEmpty());

        BigDecimal totalCredited = new BigDecimal("200.00");
        BigDecimal totalDebited = settlements.stream()
                .map(Settlement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(totalCredited, totalDebited);
    }
}
