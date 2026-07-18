package com.nova.finance.analytics;

import com.nova.finance.analytics.web.dto.BudgetAnalyticsResponse;
import com.nova.finance.analytics.web.dto.CashFlowPoint;
import com.nova.finance.analytics.web.dto.CashFlowResponse;
import com.nova.finance.analytics.web.dto.CategoryAnalysisResponse;
import com.nova.finance.analytics.web.dto.CategoryBreakdownItem;
import com.nova.finance.analytics.web.dto.Granularity;
import com.nova.finance.analytics.web.dto.GoalAnalyticsResponse;
import com.nova.finance.analytics.web.dto.SpendingOverviewResponse;
import com.nova.finance.analytics.web.dto.AnalyticsOverviewResponse;
import com.nova.finance.budget.BudgetCalculationService;
import com.nova.finance.budget.web.dto.BudgetSummaryResponse;
import com.nova.finance.budget.BudgetStatus;
import com.nova.finance.goal.GoalService;
import com.nova.finance.goal.web.dto.GoalSummaryResponse;
import com.nova.finance.goal.web.dto.GoalWithProgress;
import com.nova.finance.transaction.Transaction;
import com.nova.finance.transaction.TransactionRepository;
import com.nova.finance.transaction.Transaction.Type;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The reusable Analytics domain. It centralizes every financial aggregation Nova needs
 * so future modules (Reports, OCR, AI Copilot, Premium) consume these methods instead of
 * re-deriving data independently — the Golden Rule of Phase 5.
 *
 * <p><b>Single load, many views.</b> The transaction-based sections (spending overview,
 * cash-flow trend, category breakdown) are all derived from one in-memory load of the
 * filtered rows. Budget and goal analytics are delegated to the existing
 * {@link BudgetCalculationService} and {@link GoalService} engines — no budget or goal
 * math is duplicated here.</p>
 *
 * <p><b>Filter semantics.</b> Transaction-based sections honor the full
 * {@link AnalyticsFilter} (date window, account, category). Budget and goal analytics
 * intentionally reflect each entity's <em>current</em> period and ignore the date window
 * (a budget's health in March is meaningless when inspecting June).</p>
 */
@Service
public class AnalyticsService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int DEFAULT_SPAN_MONTHS = 12;
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM");
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter YEAR_LABEL = DateTimeFormatter.ofPattern("yyyy");
    private static final WeekFields WEEK_FIELDS = WeekFields.ISO;

    private final TransactionRepository transactionRepository;
    private final BudgetCalculationService budgetCalculationService;
    private final GoalService goalService;

    public AnalyticsService(
            TransactionRepository transactionRepository,
            BudgetCalculationService budgetCalculationService,
            GoalService goalService) {
        this.transactionRepository = transactionRepository;
        this.budgetCalculationService = budgetCalculationService;
        this.goalService = goalService;
    }

    /**
     * The complete, real-data snapshot for the applied filter — composed in one pass.
     * {@code from}/{@code to} default to the trailing {@value #DEFAULT_SPAN_MONTHS} months
     * when not supplied; {@code currency} is the owner's preferred currency (echoed back
     * to the client for display).
     */
    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse overview(AnalyticsFilter filter, String currency) {
        AnalyticsFilter resolved = withDefaultWindow(filter);
        List<Row> rows = loadRows(resolved);

        SpendingOverviewResponse spending = spendingOverview(rows, currency);
        Granularity granularity = resolveGranularity(resolved.from(), resolved.to());
        CashFlowResponse cashFlow = cashFlow(resolved, rows, granularity, currency);
        CategoryAnalysisResponse categories = categoryAnalysis(rows, currency);
        BudgetAnalyticsResponse budgets = budgetAnalytics(resolved.userId(), currency);
        GoalAnalyticsResponse goals = goalAnalytics(resolved.userId(), currency);

        return new AnalyticsOverviewResponse(
                spending, cashFlow, categories, budgets, goals,
                currency, java.time.Instant.now(),
                resolved.from(), resolved.to(),
                resolved.accountId(), resolved.categoryId());
    }

    @Transactional(readOnly = true)
    public SpendingOverviewResponse spendingOverview(AnalyticsFilter filter, String currency) {
        return spendingOverview(loadRows(withDefaultWindow(filter)), currency);
    }

    @Transactional(readOnly = true)
    public CashFlowResponse cashFlow(AnalyticsFilter filter, Granularity granularity, String currency) {
        AnalyticsFilter resolved = withDefaultWindow(filter);
        Granularity effective = granularity != null ? granularity : resolveGranularity(resolved.from(), resolved.to());
        return cashFlow(resolved, loadRows(resolved), effective, currency);
    }

    @Transactional(readOnly = true)
    public CategoryAnalysisResponse categoryAnalysis(AnalyticsFilter filter, String currency) {
        return categoryAnalysis(loadRows(withDefaultWindow(filter)), currency);
    }

    @Transactional(readOnly = true)
    public BudgetAnalyticsResponse budgetAnalytics(UUID userId, String currency) {
        BudgetSummaryResponse summary = budgetCalculationService.summary(userId, currency);
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put(BudgetStatus.HEALTHY.name(), 0L);
        distribution.put(BudgetStatus.WARNING.name(), 0L);
        distribution.put(BudgetStatus.EXCEEDED.name(), 0L);
        for (var entry : summary.budgets()) {
            String status = entry.metrics().status();
            distribution.merge(status, 1L, Long::sum);
        }
        BigDecimal efficiency = summary.totalBudgeted().signum() > 0
                ? summary.totalSpent().divide(summary.totalBudgeted(), 4, RoundingMode.HALF_UP)
                    .multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new BudgetAnalyticsResponse(summary, distribution, efficiency, currency);
    }

    @Transactional(readOnly = true)
    public GoalAnalyticsResponse goalAnalytics(UUID userId, String currency) {
        GoalSummaryResponse summary = goalService.summary(userId, currency);
        List<GoalWithProgress> upcoming = summary.goals().stream()
                .filter(g -> g.goal().active())
                .filter(g -> !"ACHIEVED".equals(g.progress().status()))
                .filter(g -> g.goal().targetDate() != null)
                .sorted(Comparator.comparing(g -> g.goal().targetDate()))
                .toList();
        BigDecimal contributionTotal = summary.goals().stream()
                .filter(g -> g.goal().active())
                .map(g -> g.progress().currentAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new GoalAnalyticsResponse(summary, upcoming, contributionTotal, currency);
    }

    // -----------------------------------------------------------------------
    // Derivation from the single in-memory row load
    // -----------------------------------------------------------------------

    private SpendingOverviewResponse spendingOverview(List<Row> rows, String currency) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        for (Row row : rows) {
            if (row.type() == Type.INCOME) {
                income = income.add(row.amount());
            } else if (row.type() == Type.EXPENSE) {
                expenses = expenses.add(row.amount());
            }
        }
        BigDecimal net = income.subtract(expenses);
        BigDecimal savingsRate = income.signum() > 0
                ? net.divide(income, 4, RoundingMode.HALF_UP).multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new SpendingOverviewResponse(income, expenses, net, savingsRate, rows.size(), currency);
    }

    private CashFlowResponse cashFlow(AnalyticsFilter filter, List<Row> rows, Granularity granularity, String currency) {
        // Stable-ordered buckets (oldest first) so charts render left-to-right in time.
        Map<String, Bucket> byKey = new LinkedHashMap<>();
        for (Row row : rows) {
            String key;
            if (granularity == Granularity.WEEKLY) {
                int week = row.occurredAt().get(WEEK_FIELDS.weekOfWeekBasedYear());
                int year = row.occurredAt().get(WEEK_FIELDS.weekBasedYear());
                key = year + "-W" + String.format("%02d", week);
            } else if (granularity == Granularity.YEARLY) {
                key = String.valueOf(row.occurredAt().getYear());
            } else {
                key = row.occurredAt().format(MONTH_KEY);
            }
            Bucket bucket = byKey.computeIfAbsent(key, k -> new Bucket(key, label(granularity, row)));
            if (row.type() == Type.INCOME) {
                bucket.income = bucket.income.add(row.amount());
            } else if (row.type() == Type.EXPENSE) {
                bucket.expenses = bucket.expenses.add(row.amount());
            }
        }
        List<CashFlowPoint> points = new ArrayList<>();
        for (Bucket bucket : byKey.values()) {
            points.add(new CashFlowPoint(bucket.key, bucket.label, bucket.income, bucket.expenses,
                    bucket.income.subtract(bucket.expenses)));
        }
        return new CashFlowResponse(granularity, currency, points);
    }

    private String label(Granularity granularity, Row row) {
        return switch (granularity) {
            case WEEKLY -> "W" + String.format("%02d", row.occurredAt().get(WEEK_FIELDS.weekOfWeekBasedYear()));
            case YEARLY -> String.valueOf(row.occurredAt().getYear());
            default -> row.occurredAt().format(MONTH_LABEL);
        };
    }

    private CategoryAnalysisResponse categoryAnalysis(List<Row> rows, String currency) {
        Map<String, CategoryBreakdownItem> expenseMap = new LinkedHashMap<>();
        Map<String, CategoryBreakdownItem> incomeMap = new LinkedHashMap<>();
        BigDecimal expenseTotal = BigDecimal.ZERO;
        BigDecimal incomeTotal = BigDecimal.ZERO;

        for (Row row : rows) {
            if (row.type() != Type.EXPENSE && row.type() != Type.INCOME) {
                continue;
            }
            String name = row.categoryName() != null ? row.categoryName() : "Uncategorized";
            String color = row.color();
            String icon = row.icon();
            Map<String, CategoryBreakdownItem> target = row.type() == Type.EXPENSE ? expenseMap : incomeMap;
            CategoryBreakdownItem existing = target.get(name);
            BigDecimal next = (existing != null ? existing.amount() : BigDecimal.ZERO).add(row.amount());
            target.put(name, new CategoryBreakdownItem(name, color, icon, next, row.type().name()));
            if (row.type() == Type.EXPENSE) {
                expenseTotal = expenseTotal.add(row.amount());
            } else {
                incomeTotal = incomeTotal.add(row.amount());
            }
        }

        List<CategoryBreakdownItem> expenses = new ArrayList<>(expenseMap.values());
        expenses.sort(Comparator.comparing(CategoryBreakdownItem::amount).reversed());
        List<CategoryBreakdownItem> incomes = new ArrayList<>(incomeMap.values());
        incomes.sort(Comparator.comparing(CategoryBreakdownItem::amount).reversed());
        // Top spending categories: the five highest-expense categories.
        List<CategoryBreakdownItem> top = expenses.stream().limit(5).toList();

        return new CategoryAnalysisResponse(expenseTotal, incomeTotal, expenses, incomes, top, currency);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private AnalyticsFilter withDefaultWindow(AnalyticsFilter filter) {
        OffsetDateTime from = filter.from();
        OffsetDateTime to = filter.to();
        if (from == null || to == null) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            if (from == null) {
                from = now.minusMonths(DEFAULT_SPAN_MONTHS).withDayOfMonth(1).toLocalDate()
                        .atStartOfDay().atOffset(ZoneOffset.UTC);
            }
            if (to == null) {
                to = now.plusMonths(1).withDayOfMonth(1).toLocalDate()
                        .atStartOfDay().atOffset(ZoneOffset.UTC);
            }
        }
        return new AnalyticsFilter(filter.userId(), from, to, filter.accountId(), filter.categoryId());
    }

    private Granularity resolveGranularity(OffsetDateTime from, OffsetDateTime to) {
        long days = java.time.Duration.between(from, to).toDays();
        if (days < 84) { // under ~12 weeks
            return Granularity.WEEKLY;
        }
        if (days > 730) { // over ~2 years
            return Granularity.YEARLY;
        }
        return Granularity.MONTHLY;
    }

    private List<Row> loadRows(AnalyticsFilter filter) {
        List<Object[]> raw = transactionRepository.loadAnalyticsRows(
                filter.userId(), filter.from(), filter.to(), filter.accountId(), filter.categoryId());
        List<Row> rows = new ArrayList<>(raw.size());
        for (Object[] r : raw) {
            rows.add(new Row(
                    (OffsetDateTime) r[0],
                    (Transaction.Type) r[1],
                    (BigDecimal) r[2],
                    (UUID) r[3],
                    (String) r[4],
                    (String) r[5],
                    (String) r[6]));
        }
        return rows;
    }

    /** A projected transaction row for in-memory aggregation. */
    private record Row(
            OffsetDateTime occurredAt,
            Transaction.Type type,
            BigDecimal amount,
            UUID categoryId,
            String categoryName,
            String color,
            String icon
    ) {
    }

    /** Mutable accumulator for one cash-flow bucket. */
    private static final class Bucket {
        final String key;
        final String label;
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;

        Bucket(String key, String label) {
            this.key = key;
            this.label = label;
        }
    }
}
