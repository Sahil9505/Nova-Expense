package com.nova.finance.budget;

import com.nova.common.exception.ResourceNotFoundException;
import com.nova.finance.budget.web.dto.BudgetMetrics;
import com.nova.finance.budget.web.dto.BudgetMetricsResponse;
import com.nova.finance.budget.web.dto.BudgetResponse;
import com.nova.finance.budget.web.dto.BudgetSummaryResponse;
import com.nova.finance.transaction.Transaction;
import com.nova.finance.transaction.TransactionRepository;
import com.nova.finance.transaction.Transaction.Type;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The reusable Budget Intelligence engine. It resolves each budget's current period
 * window, aggregates real expense transactions against it (category-scoped or overall),
 * and classifies health via {@link BudgetCalculator}.
 *
 * <p><b>Performance.</b> A summary covering many budgets still issues exactly one
 * expense query for the union of their windows, then buckets the rows in memory per
 * budget — no per-budget round-trip, no N+1, and never recomputes a value inside a
 * request. This mirrors how the dashboard aggregates in a single read-only pass.</p>
 *
 * <p><b>Reuse.</b> Analytics, Financial Goals, and AI phases are expected to call
 * {@link #summary}/{@link #metrics} (or the pure {@link BudgetCalculator}) directly
 * rather than re-deriving budget math.</p>
 */
@Service
public class BudgetCalculationService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetProperties thresholds;
    private final BudgetMapper budgetMapper;

    public BudgetCalculationService(
            BudgetRepository budgetRepository,
            TransactionRepository transactionRepository,
            BudgetProperties thresholds,
            BudgetMapper budgetMapper) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.thresholds = thresholds;
        this.budgetMapper = budgetMapper;
    }

    /**
     * The Budget Intelligence overview for a user: the four roll-up figures
     * (active budgets, total budgeted, total spent, remaining) plus per-budget detail
     * with live metrics. A single expense query covers the union of all windows.
     *
     * @param currency the owner's preferred currency, echoed to the client for display
     */
    @Transactional(readOnly = true)
    public BudgetSummaryResponse summary(UUID userId, String currency) {
        return summary(userId, currency, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public BudgetSummaryResponse summary(UUID userId, String currency, LocalDate today) {
        List<Budget> budgets = budgetRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (budgets.isEmpty()) {
            return new BudgetSummaryResponse(0, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, currency, 0, 0, List.of());
        }

        // Resolve every budget's window once, recording the outer span we must load.
        List<Resolved> resolved = new ArrayList<>();
        LocalDate outerStart = budgets.get(0).getStartDate();
        LocalDate outerEnd = budgets.get(0).getStartDate();
        for (Budget budget : budgets) {
            BudgetPeriods.DateRange range = BudgetPeriods.resolve(budget, today);
            resolved.add(new Resolved(budget, range));
            if (range.startInclusive().isBefore(outerStart)) {
                outerStart = range.startInclusive();
            }
            if (range.endExclusive().isAfter(outerEnd)) {
                outerEnd = range.endExclusive();
            }
        }

        // One load for the whole span, then bucket per budget in memory.
        List<Object[]> rows = transactionRepository.findExpenseRows(
                userId, outerStart.atStartOfDay().atOffset(ZoneOffset.UTC),
                outerEnd.atStartOfDay().atOffset(ZoneOffset.UTC));

        List<BudgetMetricsResponse> detail = new ArrayList<>();
        int activeBudgets = 0;
        int warningCount = 0;
        int exceededCount = 0;
        BigDecimal totalBudgeted = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;

        for (Resolved entry : resolved) {
            BudgetCalculation calc = compute(entry.budget, entry.range, rows);
            detail.add(new BudgetMetricsResponse(
                    budgetMapper.toResponse(entry.budget), BudgetMetrics.from(calc)));
            if (entry.budget.isActive()) {
                activeBudgets++;
                totalBudgeted = totalBudgeted.add(calc.amount());
                totalSpent = totalSpent.add(calc.spent());
                if (calc.status() == BudgetStatus.WARNING) {
                    warningCount++;
                } else if (calc.status() == BudgetStatus.EXCEEDED) {
                    exceededCount++;
                }
            }
        }

        BigDecimal totalRemaining = totalBudgeted.subtract(totalSpent);
        return new BudgetSummaryResponse(activeBudgets, totalBudgeted, totalSpent,
                totalRemaining, currency, warningCount, exceededCount, detail);
    }

    /** Per-budget intelligence for a single owned budget, resolved to "today". */
    @Transactional(readOnly = true)
    public BudgetMetricsResponse metrics(UUID userId, UUID budgetId) {
        return metrics(userId, budgetId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public BudgetMetricsResponse metrics(UUID userId, UUID budgetId, LocalDate today) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", budgetId.toString()));
        BudgetPeriods.DateRange range = BudgetPeriods.resolve(budget, today);
        List<Object[]> rows = transactionRepository.findExpenseRows(
                userId, range.startInstant(), range.endInstant());
        BudgetCalculation calc = compute(budget, range, rows);
        return new BudgetMetricsResponse(budgetMapper.toResponse(budget), BudgetMetrics.from(calc));
    }

    /** Aggregates the loaded rows for one budget's window + scope and classifies it. */
    private BudgetCalculation compute(Budget budget, BudgetPeriods.DateRange range, List<Object[]> rows) {
        UUID targetCategory = budget.getCategory() != null ? budget.getCategory().getId() : null;
        BigDecimal spent = BigDecimal.ZERO;
        for (Object[] row : rows) {
            UUID rowCategory = (UUID) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            OffsetDateTime occurredAt = (OffsetDateTime) row[2];
            if (!range.contains(occurredAt)) {
                continue;
            }
            boolean matchesScope = targetCategory == null
                    ? true // overall budget: every expense counts
                    : targetCategory.equals(rowCategory); // category budget: only its category
            if (matchesScope) {
                spent = spent.add(amount);
            }
        }
        return BudgetCalculator.evaluate(budget.getAmount(), spent, thresholds);
    }

    /** A budget paired with its resolved date window. */
    private record Resolved(Budget budget, BudgetPeriods.DateRange range) {
    }
}
