package com.nova.ai;

import com.nova.ai.dto.ChatResponse;
import com.nova.ai.dto.DataReference;
import com.nova.finance.budget.web.dto.BudgetSummaryResponse;
import com.nova.finance.goal.web.dto.GoalSummaryResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps a {@link FinancialContext} and the detected {@link IntentType} into a small,
 * render-ready {@link DataReference} the UI shows beneath the assistant's answer.
 * This keeps the numbers the user sees identical to the figures the model was
 * grounded on — the AI never presents a figure it didn't receive from a domain
 * service.
 */
@Component
public class AiResponseMapper {

    public DataReference toReference(FinancialContext ctx, IntentType intent) {
        if (ctx == null) {
            return null;
        }
        return switch (intent) {
            case SPENDING, CASH_FLOW, FINANCIAL_HEALTH, GENERAL_SUMMARY -> spendingReference(ctx);
            case COMPARISON -> spendingReference(ctx);
            case BUDGET -> budgetReference(ctx);
            case GOALS -> goalReference(ctx);
            case RECEIPTS -> receiptReference(ctx);
        };
    }

    private DataReference spendingReference(FinancialContext ctx) {
        if (ctx.overview() == null || ctx.overview().spendingOverview() == null) {
            return null;
        }
        var s = ctx.overview().spendingOverview();
        List<DataReference.Fact> facts = new ArrayList<>();
        facts.add(new DataReference.Fact("Income", money(s.income(), ctx.currency())));
        facts.add(new DataReference.Fact("Expenses", money(s.expenses(), ctx.currency())));
        facts.add(new DataReference.Fact("Net cash flow", money(s.netCashFlow(), ctx.currency())));
        facts.add(new DataReference.Fact("Savings rate", pct(s.savingsRatePct())));

        List<DataReference.DataRow> items = new ArrayList<>();
        if (ctx.overview().categoryAnalysis() != null
                && ctx.overview().categoryAnalysis().topCategories() != null) {
            for (var c : ctx.overview().categoryAnalysis().topCategories()) {
                items.add(new DataReference.DataRow(c.name(), money(c.amount(), ctx.currency()), c.type()));
            }
        }
        return new DataReference("spending", "This month at a glance", facts, items);
    }

    private DataReference budgetReference(FinancialContext ctx) {
        BudgetSummaryResponse b = ctx.budgets();
        if (b == null) {
            return null;
        }
        List<DataReference.Fact> facts = new ArrayList<>();
        facts.add(new DataReference.Fact("Total budgeted", money(b.totalBudgeted(), ctx.currency())));
        facts.add(new DataReference.Fact("Total spent", money(b.totalSpent(), ctx.currency())));
        facts.add(new DataReference.Fact("Total remaining", money(b.totalRemaining(), ctx.currency())));

        List<DataReference.DataRow> items = new ArrayList<>();
        if (b.budgets() != null) {
            for (var m : b.budgets()) {
                items.add(new DataReference.DataRow(
                        m.budget().name(),
                        money(m.metrics().spent(), ctx.currency()) + " / " + money(m.metrics().amount(), ctx.currency()),
                        m.metrics().status()));
            }
        }
        return new DataReference("budget", "Budget status", facts, items);
    }

    private DataReference goalReference(FinancialContext ctx) {
        GoalSummaryResponse g = ctx.goals();
        if (g == null) {
            return null;
        }
        List<DataReference.Fact> facts = new ArrayList<>();
        facts.add(new DataReference.Fact("Overall progress", pct(g.overallPercent())));
        facts.add(new DataReference.Fact("Active goals", String.valueOf(g.activeGoals())));
        facts.add(new DataReference.Fact("Achieved", String.valueOf(g.achievedGoals())));

        List<DataReference.DataRow> items = new ArrayList<>();
        if (g.goals() != null) {
            for (var gw : g.goals()) {
                items.add(new DataReference.DataRow(
                        gw.goal().name(),
                        pct(gw.progress().percentageComplete()),
                        gw.progress().status()));
            }
        }
        return new DataReference("goal", "Goal progress", facts, items);
    }

    private DataReference receiptReference(FinancialContext ctx) {
        if (ctx.receipts() == null || ctx.receipts().isEmpty()) {
            return new DataReference("receipt", "Recent receipts", List.of(), List.of());
        }
        List<DataReference.DataRow> items = new ArrayList<>();
        for (var r : ctx.receipts()) {
            items.add(new DataReference.DataRow(
                    r.filename(),
                    r.status(),
                    r.linkedTransactionId() != null ? "linked" : "not linked"));
        }
        return new DataReference("receipt", "Recent receipts", List.of(), items);
    }

    public ChatResponse buildResponse(
            java.util.UUID conversationId, String answer, IntentType intent,
            DataReference reference, List<String> suggestions) {
        return new ChatResponse(conversationId, answer, intent, reference, suggestions);
    }

    private String money(BigDecimal value, String currency) {
        if (value == null) {
            return "n/a";
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " " + currency;
    }

    private String pct(BigDecimal value) {
        if (value == null) {
            return "n/a";
        }
        return value.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
