package com.nova.ai;

/**
 * The set of questions the AI Financial Copilot understands. Detection is
 * intentionally lightweight (keyword-scored, no model call) so intent resolution
 * is cheap and deterministic. New intents are added here and registered in
 * {@link IntentResolver} without changing the rest of the pipeline.
 *
 * <p>Each intent maps to a focused data-gathering strategy in
 * {@code FinancialContextBuilder}; the AI never queries the database directly and
 * never recalculates financial figures — it explains data produced by the
 * existing domains (analytics, budgets, goals, receipts, dashboard).</p>
 */
public enum IntentType {
    SPENDING("Spending"),
    BUDGET("Budgets"),
    GOALS("Goals"),
    RECEIPTS("Receipts"),
    CASH_FLOW("Cash flow"),
    FINANCIAL_HEALTH("Financial health"),
    COMPARISON("Comparison"),
    GENERAL_SUMMARY("General summary");

    private final String label;

    IntentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
