package com.nova.finance.budget;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolves the calendar window a budget's spend is measured against. Recurring budgets
 * use the current week/month/year (anchored to {@code referenceDate}); CUSTOM uses its
 * own inclusive range (made half-open for querying).
 */
class BudgetPeriodsTest {

    private final LocalDate ref = LocalDate.of(2026, 7, 17); // a Friday

    private Budget custom(LocalDate start, LocalDate end) {
        Budget b = new Budget(null, "Trip", new java.math.BigDecimal("100"), Budget.Period.CUSTOM, start);
        b.setEndDate(end);
        return b;
    }

    @Test
    void monthlyUsesReferenceMonth() {
        BudgetPeriods.DateRange range = BudgetPeriods.resolve(
                new Budget(null, "M", new java.math.BigDecimal("1"), Budget.Period.MONTHLY, ref), ref);
        assertThat(range.startInclusive()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(range.endExclusive()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    void yearlyUsesReferenceYear() {
        BudgetPeriods.DateRange range = BudgetPeriods.resolve(
                new Budget(null, "Y", new java.math.BigDecimal("1"), Budget.Period.YEARLY, ref), ref);
        assertThat(range.startInclusive()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(range.endExclusive()).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    void weeklyStartsOnMonday() {
        // 2026-07-17 is a Friday; the window must start the preceding Monday.
        BudgetPeriods.DateRange range = BudgetPeriods.resolve(
                new Budget(null, "W", new java.math.BigDecimal("1"), Budget.Period.WEEKLY, ref), ref);
        assertThat(range.startInclusive().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(range.startInclusive()).isEqualTo(LocalDate.of(2026, 7, 13));
        assertThat(range.endExclusive()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    void weeklyOnMondayStillStartsThatMonday() {
        LocalDate monday = LocalDate.of(2026, 7, 13);
        BudgetPeriods.DateRange range = BudgetPeriods.resolve(
                new Budget(null, "W", new java.math.BigDecimal("1"), Budget.Period.WEEKLY, monday), monday);
        assertThat(range.startInclusive()).isEqualTo(monday);
    }

    @Test
    void customRangeIsHalfOpenAndInclusiveToEnd() {
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 31); // user's inclusive end
        BudgetPeriods.DateRange range = BudgetPeriods.resolve(custom(start, end), ref);
        assertThat(range.startInclusive()).isEqualTo(start);
        // The inclusive end date is included, so the exclusive boundary is the next day.
        assertThat(range.endExclusive()).isEqualTo(end.plusDays(1));

        // midnight of the last day is inside; midnight of the next day is outside.
        assertThat(range.contains(end.atStartOfDay().atOffset(ZoneOffset.UTC))).isTrue();
        assertThat(range.contains(end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC))).isFalse();
    }

    @Test
    void windowBoundariesAreInclusiveStartExclusiveEnd() {
        BudgetPeriods.DateRange range = BudgetPeriods.resolve(
                new Budget(null, "M", new java.math.BigDecimal("1"), Budget.Period.MONTHLY, ref), ref);
        OffsetDateTime firstInstant = LocalDate.of(2026, 7, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime lastInstant = LocalDate.of(2026, 7, 31).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
        OffsetDateTime nextMonth = LocalDate.of(2026, 8, 1).atStartOfDay().atOffset(ZoneOffset.UTC);

        assertThat(range.contains(firstInstant)).isTrue();
        assertThat(range.contains(lastInstant)).isTrue();
        assertThat(range.contains(nextMonth)).isFalse();
    }

    @Test
    void windowIgnoresBudgetStartDateForRecurring() {
        // A monthly budget created in January still measures July when reference is July.
        Budget b = new Budget(null, "M", new java.math.BigDecimal("1"), Budget.Period.MONTHLY,
                LocalDate.of(2026, 1, 15));
        BudgetPeriods.DateRange range = BudgetPeriods.resolve(b, ref);
        assertThat(range.startInclusive()).isEqualTo(LocalDate.of(2026, 7, 1));
    }
}
