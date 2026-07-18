package com.nova.ai;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Resolves the "this month" / "last month" / "this year" style references the
 * copilot understands into UTC half-open windows ({@code [from, to)}), matching
 * the convention used everywhere else in Nova so day-boundary transactions are
 * never double-counted. This is the only place the AI layer reasons about dates;
 * it never recomputes financial aggregates.
 */
@Component
public class PeriodResolver {

    private static final ZoneOffset UTC = ZoneOffset.UTC;

    /** Current calendar month, from the 1st at 00:00 UTC to the 1st of next month. */
    public Window currentMonth() {
        LocalDate start = LocalDate.now(UTC).withDayOfMonth(1);
        return new Window(start.atStartOfDay().atOffset(UTC),
                start.plusMonths(1).atStartOfDay().atOffset(UTC));
    }

    /** The month immediately before {@link #currentMonth()}. */
    public Window previousMonth() {
        LocalDate start = LocalDate.now(UTC).withDayOfMonth(1).minusMonths(1);
        return new Window(start.atStartOfDay().atOffset(UTC),
                start.plusMonths(1).atStartOfDay().atOffset(UTC));
    }

    /** Calendar year to date, from January 1st. */
    public Window yearToDate() {
        LocalDate start = LocalDate.now(UTC).withDayOfYear(1);
        return new Window(start.atStartOfDay().atOffset(UTC),
                LocalDate.now(UTC).plus(1, ChronoUnit.DAYS).atStartOfDay().atOffset(UTC));
    }

    public record Window(OffsetDateTime from, OffsetDateTime to) {
        public String label() {
            return from.toLocalDate().withDayOfMonth(1) + " .. " + to.toLocalDate();
        }
    }
}
