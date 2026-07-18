package com.nova.ai.dto;

/**
 * A compact, render-ready summary of the figures the assistant used to answer.
 * The client shows this as a small "data card" beneath the explanation. It keeps
 * the UI honest: every number it displays was produced by Nova's own domains, not
 * invented by the model.
 *
 * @param kind   a stable key the frontend switches on (e.g. "spending", "budget",
 *               "goal", "receipt", "cashflow", "health") for icon/colour choices
 * @param title  a human label for the card
 * @param facts  ordered label/value pairs; value is already formatted for display
 * @param items  optional structured rows (e.g. top categories, budgets at risk)
 */
public record DataReference(
        String kind,
        String title,
        java.util.List<Fact> facts,
        java.util.List<DataRow> items
) {

    public record Fact(String label, String value) {
    }

    public record DataRow(String label, String value, String hint) {
    }
}
