package com.nova.finance.budget;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure math for budget health: remaining, percentage used, and status at and around
 * the configurable thresholds. No Spring context — this is the single source of truth
 * for the calculation that every budget surface reuses.
 */
class BudgetCalculatorTest {

    private final BudgetProperties thresholds = new BudgetProperties(null, null); // defaults 0.80 / 1.00

    @Test
    void healthyWhenUnderWarningThreshold() {
        BudgetCalculation calc = BudgetCalculator.evaluate(new BigDecimal("100.00"), new BigDecimal("50.00"), thresholds);
        assertThat(calc.status()).isEqualTo(BudgetStatus.HEALTHY);
        assertThat(calc.percentageUsed()).isEqualByComparingTo("50.00");
        assertThat(calc.remaining()).isEqualByComparingTo("50.00");
    }

    @Test
    void warningAtExactly80Percent() {
        BudgetCalculation calc = BudgetCalculator.evaluate(new BigDecimal("100.00"), new BigDecimal("80.00"), thresholds);
        assertThat(calc.status()).isEqualTo(BudgetStatus.WARNING);
        assertThat(calc.percentageUsed()).isEqualByComparingTo("80.00");
        assertThat(calc.remaining()).isEqualByComparingTo("20.00");
    }

    @Test
    void warningInThe99PercentBand() {
        BudgetCalculation calc = BudgetCalculator.evaluate(new BigDecimal("100.00"), new BigDecimal("99.99"), thresholds);
        assertThat(calc.status()).isEqualTo(BudgetStatus.WARNING);
        assertThat(calc.percentageUsed()).isEqualByComparingTo("99.99");
    }

    @Test
    void exceededAtExactly100Percent() {
        BudgetCalculation calc = BudgetCalculator.evaluate(new BigDecimal("100.00"), new BigDecimal("100.00"), thresholds);
        assertThat(calc.status()).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(calc.percentageUsed()).isEqualByComparingTo("100.00");
        assertThat(calc.remaining()).isEqualByComparingTo("0.00");
    }

    @Test
    void exceededWhenOverBudget() {
        BudgetCalculation calc = BudgetCalculator.evaluate(new BigDecimal("100.00"), new BigDecimal("150.00"), thresholds);
        assertThat(calc.status()).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(calc.percentageUsed()).isEqualByComparingTo("150.00");
        // Remaining is negative once over budget — the UI surfaces the overspend.
        assertThat(calc.remaining()).isEqualByComparingTo("-50.00");
    }

    @Test
    void zeroSpendingOnNonZeroBudgetIsHealthy() {
        BudgetCalculation calc = BudgetCalculator.evaluate(new BigDecimal("100.00"), BigDecimal.ZERO, thresholds);
        assertThat(calc.status()).isEqualTo(BudgetStatus.HEALTHY);
        assertThat(calc.percentageUsed()).isEqualByComparingTo("0.00");
        assertThat(calc.remaining()).isEqualByComparingTo("100.00");
    }

    @Test
    void nullSpentIsTreatedAsZero() {
        BudgetCalculation calc = BudgetCalculator.evaluate(new BigDecimal("100.00"), null, thresholds);
        assertThat(calc.status()).isEqualTo(BudgetStatus.HEALTHY);
        assertThat(calc.spent()).isEqualByComparingTo("0.00");
        assertThat(calc.remaining()).isEqualByComparingTo("100.00");
    }

    @Test
    void nonPositiveAmountWithSpendIsExceeded() {
        // A guard case: an unlimited/zero limit with any spend is considered blown.
        BudgetCalculation calc = BudgetCalculator.evaluate(BigDecimal.ZERO, new BigDecimal("10.00"), thresholds);
        assertThat(calc.status()).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(calc.percentageUsed()).isEqualByComparingTo("0.00");
    }

    @Test
    void nonPositiveAmountWithNoSpendIsHealthy() {
        BudgetCalculation calc = BudgetCalculator.evaluate(BigDecimal.ZERO, BigDecimal.ZERO, thresholds);
        assertThat(calc.status()).isEqualTo(BudgetStatus.HEALTHY);
    }

    @Test
    void percentageScaledToTwoDecimals() {
        // 1 / 3 = 0.3333... -> 33.33%
        BudgetCalculation calc = BudgetCalculator.evaluate(new BigDecimal("3.00"), new BigDecimal("1.00"), thresholds);
        assertThat(calc.percentageUsed()).isEqualByComparingTo("33.33");
    }

    @Test
    void customThresholdsAreRespected() {
        // Tighten to warning at 50%, exceeded at 90%.
        BudgetProperties tight = new BudgetProperties(new BigDecimal("0.50"), new BigDecimal("0.90"));
        BudgetCalculation atHalf = BudgetCalculator.evaluate(new BigDecimal("100.00"), new BigDecimal("50.00"), tight);
        assertThat(atHalf.status()).isEqualTo(BudgetStatus.WARNING);

        BudgetCalculation atNinety = BudgetCalculator.evaluate(new BigDecimal("100.00"), new BigDecimal("90.00"), tight);
        assertThat(atNinety.status()).isEqualTo(BudgetStatus.EXCEEDED);
    }
}
