package com.nova.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IntentResolverTest {

    private final IntentResolver resolver = new IntentResolver();

    @Test
    void resolvesSpendingKeywords() {
        assertEquals(IntentType.SPENDING, resolver.resolve("Where did I spend the most money this month?", null));
        assertEquals(IntentType.SPENDING, resolver.resolve("How much did I spend on food?", null));
        assertEquals(IntentType.SPENDING, resolver.resolve("What are my biggest subscriptions?", null));
    }

    @Test
    void resolvesBudgetKeywords() {
        assertEquals(IntentType.BUDGET, resolver.resolve("Which budgets are close to being exhausted?", null));
        assertEquals(IntentType.BUDGET, resolver.resolve("Am I over budget on groceries?", null));
    }

    @Test
    void resolvesGoalKeywords() {
        assertEquals(IntentType.GOALS, resolver.resolve("Which goal is closest to completion?", null));
        assertEquals(IntentType.GOALS, resolver.resolve("How is my emergency fund doing?", null));
    }

    @Test
    void resolvesReceiptKeywords() {
        assertEquals(IntentType.RECEIPTS, resolver.resolve("Show my recent receipts", null));
    }

    @Test
    void resolvesCashFlowKeywords() {
        IntentType r = resolver.resolve("How much did I save this month?", null);
        System.out.println("DEBUG cashflow resolve='How much did I save this month?' => " + r);
        assertEquals(IntentType.CASH_FLOW, r);
        assertEquals(IntentType.CASH_FLOW, resolver.resolve("How much can I still spend safely?", null));
    }

    @Test
    void resolvesComparisonKeywords() {
        assertEquals(IntentType.COMPARISON, resolver.resolve("Compare this month with last month", null));
        assertEquals(IntentType.COMPARISON, resolver.resolve("Why did my spending increase?", null));
    }

    @Test
    void resolvesHealthKeywords() {
        assertEquals(IntentType.FINANCIAL_HEALTH, resolver.resolve("How healthy are my finances?", null));
        assertEquals(IntentType.FINANCIAL_HEALTH, resolver.resolve("Am I on track?", null));
    }

    @Test
    void groundsFollowUpsToPreviousIntentWhenNoKeywordMatches() {
        assertEquals(IntentType.BUDGET, resolver.resolve("what about last month?", IntentType.BUDGET));
        assertEquals(IntentType.GOALS, resolver.resolve("and the other one?", IntentType.GOALS));
    }

    @Test
    void fallsBackToGeneralSummaryWithoutSignal() {
        assertEquals(IntentType.GENERAL_SUMMARY, resolver.resolve("tell me a joke", null));
        assertEquals(IntentType.GENERAL_SUMMARY, resolver.resolve("hello", null));
    }
}
