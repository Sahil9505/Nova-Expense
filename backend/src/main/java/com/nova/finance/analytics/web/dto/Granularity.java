package com.nova.finance.analytics.web.dto;

/**
 * The time bucket a cash-flow trend is aggregated into. Chosen by the controller from
 * the applied range so the chart stays readable: narrow windows (under ~12 weeks) are
 * shown weekly, multi-year windows yearly, and everything in between monthly.
 */
public enum Granularity {
    WEEKLY,
    MONTHLY,
    YEARLY
}
