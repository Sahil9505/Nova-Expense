package com.nova.finance.budget;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Resolves the concrete date window a budget's spending is measured against.
 *
 * <p>Recurring budgets ({@code WEEKLY}, {@code MONTHLY}, {@code YEARLY}) are evaluated
 * against the calendar period that contains the reference date ("this week / month /
 * year"), which is the intelligence users expect on a live dashboard. A {@code CUSTOM}
 * budget uses its own explicit {@code [startDate, endDate]} range. Weeks start on
 * Monday (ISO-8601), matching the rest of Nova's date handling.</p>
 *
 * <p>All windows are half-open on dates ({@code startInclusive}, {@code endExclusive})
 * so that day-boundary transactions are never double-counted, then projected to UTC
 * instants for querying transactions, consistent with the dashboard aggregation.</p>
 */
public final class BudgetPeriods {

    private BudgetPeriods() {
    }

    /** A half-open date range: {@code startInclusive <= day < endExclusive}. */
    public record DateRange(LocalDate startInclusive, LocalDate endExclusive) {

        /** Start of the window as a UTC instant (inclusive). */
        public OffsetDateTime startInstant() {
            return startInclusive.atStartOfDay().atOffset(ZoneOffset.UTC);
        }

        /** End of the window as a UTC instant (exclusive). */
        public OffsetDateTime endInstant() {
            return endExclusive.atStartOfDay().atOffset(ZoneOffset.UTC);
        }

        /** True when the given instant falls inside this window. */
        public boolean contains(OffsetDateTime instant) {
            return !instant.isBefore(startInstant()) && instant.isBefore(endInstant());
        }
    }

    /**
     * Resolves the window for a budget relative to {@code referenceDate} ("today").
     *
     * @throws IllegalArgumentException if a CUSTOM budget is missing its date range
     */
    public static DateRange resolve(Budget budget, LocalDate referenceDate) {
        return switch (budget.getPeriod()) {
            case WEEKLY -> {
                LocalDate start = referenceDate.with(DayOfWeek.MONDAY);
                yield new DateRange(start, start.plusWeeks(1));
            }
            case MONTHLY -> {
                LocalDate start = referenceDate.withDayOfMonth(1);
                yield new DateRange(start, start.plusMonths(1));
            }
            case YEARLY -> {
                LocalDate start = referenceDate.withDayOfYear(1);
                yield new DateRange(start, start.plusYears(1));
            }
            case CUSTOM -> {
                if (budget.getStartDate() == null || budget.getEndDate() == null) {
                    throw new IllegalArgumentException(
                            "Custom budget " + budget.getId() + " is missing its date range.");
                }
                // endDate is inclusive for the user; make the window exclusive by adding a day.
                yield new DateRange(budget.getStartDate(), budget.getEndDate().plusDays(1));
            }
        };
    }
}
