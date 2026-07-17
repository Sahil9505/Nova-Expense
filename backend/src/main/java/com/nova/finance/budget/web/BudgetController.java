package com.nova.finance.budget.web;

import com.nova.auth.security.NovaUserPrincipal;
import com.nova.common.api.ApiResponse;
import com.nova.finance.budget.BudgetCalculationService;
import com.nova.finance.budget.BudgetService;
import com.nova.finance.budget.web.dto.BudgetMetricsResponse;
import com.nova.finance.budget.web.dto.BudgetResponse;
import com.nova.finance.budget.web.dto.BudgetSummaryResponse;
import com.nova.finance.budget.web.dto.CreateBudgetRequest;
import com.nova.finance.budget.web.dto.UpdateBudgetRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetService budgetService;
    private final BudgetCalculationService budgetCalculationService;

    public BudgetController(
            BudgetService budgetService,
            BudgetCalculationService budgetCalculationService) {
        this.budgetService = budgetService;
        this.budgetCalculationService = budgetCalculationService;
    }

    @GetMapping
    public ApiResponse<List<BudgetResponse>> listBudgets(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @RequestParam(required = false) Boolean active) {
        return ApiResponse.ok(budgetService.listBudgets(principal.getUserId(), active));
    }

    @PostMapping
    public ApiResponse<BudgetResponse> createBudget(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @Valid @RequestBody CreateBudgetRequest request) {
        return ApiResponse.ok("Budget created.", budgetService.createBudget(principal.getUserId(), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<BudgetResponse> getBudget(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @PathVariable UUID id) {
        return ApiResponse.ok(budgetService.getBudget(principal.getUserId(), id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<BudgetResponse> updateBudget(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBudgetRequest request) {
        return ApiResponse.ok("Budget updated.", budgetService.updateBudget(principal.getUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBudget(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @PathVariable UUID id) {
        budgetService.deleteBudget(principal.getUserId(), id);
        return ApiResponse.ok("Budget deleted.");
    }

    @GetMapping("/summary")
    public ApiResponse<BudgetSummaryResponse> budgetSummary(
            @AuthenticationPrincipal NovaUserPrincipal principal) {
        // Currency is re-fetched inside the service transaction; "USD" is a safe
        // default the service overrides before building the response.
        return ApiResponse.ok(budgetCalculationService.summary(principal.getUserId(), "USD"));
    }

    @GetMapping("/{id}/metrics")
    public ApiResponse<BudgetMetricsResponse> budgetMetrics(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @PathVariable UUID id) {
        return ApiResponse.ok(budgetCalculationService.metrics(principal.getUserId(), id));
    }
}
