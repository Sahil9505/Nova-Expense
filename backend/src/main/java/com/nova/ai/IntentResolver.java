package com.nova.ai;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Lightweight, deterministic intent detection. It scores each known
 * {@link IntentType} against keyword hits in the user's question and returns the
 * highest-scoring intent (ties broken by the enum declaration order, which places
 * the more specific intents first). No model call is involved, so resolution is
 * instant and free.
 *
 * <p>Follow-up questions ("what about last month?", "and the budgets?") often lack
 * strong keywords; for those the caller passes the previous intent so they can be
 * grounded to the same data domain. When even that is absent we fall back to
 * {@link IntentType#GENERAL_SUMMARY}, which pulls the broad dashboard/overview
 * context.</p>
 *
 * <p>Designed to be extended: add an entry to {@link #KEYWORDS} (and the enum) to
 * support a new question family — nothing else in the pipeline needs to change.</p>
 */
@Component
public class IntentResolver {

    private static final Map<IntentType, String[]> KEYWORDS = new LinkedHashMap<>();

    static {
        // Declared most-specific first. The resolver returns the first (highest
        // priority) intent that reaches the best score, so a tie between a specific
        // intent (e.g. COMPARISON) and the broad SPENDING bucket is broken in favour
        // of the specific one. SPENDING is intentionally near the end: its keywords
        // ("spend", "spending") are short and common, so it should only win when no
        // more specific intent matched.
        KEYWORDS.put(IntentType.BUDGET, new String[]{
                "budget", "budgets", "spending limit", "overspend", "overspent",
                "exhausted", "remaining budget", "over budget", "allocation", "what about"
        });
        KEYWORDS.put(IntentType.GOALS, new String[]{
                "goal", "goals", "saving goal", "target", "saved up", "close to completion",
                "achieve", "progress", "contribution", "fund", "saving"
        });
        KEYWORDS.put(IntentType.RECEIPTS, new String[]{
                "receipt", "receipts", "scanned", "capture", "uploaded receipt", "ocr"
        });
        KEYWORDS.put(IntentType.COMPARISON, new String[]{
                "compare", "comparison", "versus", "vs", "difference", "last month",
                "previous month", "this month compared", "compared to", "change", "increased",
                "decreased", "why did", "trend"
        });
        KEYWORDS.put(IntentType.CASH_FLOW, new String[]{
                "cash flow", "income", "earn", "earned", "salary", "net", "savings",
                "save this month", "how much did i save", "money left", "left to spend",
                "can i spend", "safe to spend", "safe", "safely"
        });
        KEYWORDS.put(IntentType.FINANCIAL_HEALTH, new String[]{
                "health", "healthy", "how am i doing", "financially", "overview", "summary",
                "status", "am i on track", "situation"
        });
        KEYWORDS.put(IntentType.SPENDING, new String[]{
                "spend", "spent", "expense", "expenses", "cost", "cost me",
                "largest", "biggest", "most money", "where did", "top category", "subscription",
                "subscriptions", "recurring"
        });
        KEYWORDS.put(IntentType.GENERAL_SUMMARY, new String[]{
                "what's up", "how are things", "tell me about"
        });
    }

    /**
     * Resolve the intent for a question.
     *
     * @param question      the user's current message (lower-cased internally)
     * @param previousIntent intent of the prior turn, used to ground follow-ups
     */
    public IntentType resolve(String question, IntentType previousIntent) {
        String text = question == null ? "" : question.toLowerCase();

        int bestScore = 0;
        IntentType best = null;
        for (Map.Entry<IntentType, String[]> entry : KEYWORDS.entrySet()) {
            int score = score(entry.getValue(), text);
            // `>` (not `>=`) means a tie is broken in favour of the first-declared
            // intent. The map is ordered most-specific first, so on equal scores the
            // specific intent (e.g. CASH_FLOW) wins over the broad SPENDING bucket.
            if (score > bestScore) {
                bestScore = score;
                best = entry.getKey();
            }
        }

        if (best != null) {
            return best;
        }
        // No keyword matched. Ground a likely follow-up to the previous domain;
        // otherwise give the broadest context.
        return previousIntent != null ? previousIntent : IntentType.GENERAL_SUMMARY;
    }

    private int score(String[] words, String text) {
        int hits = 0;
        for (String word : words) {
            if (text.contains(word)) {
                hits++;
            }
        }
        return hits;
    }
}
