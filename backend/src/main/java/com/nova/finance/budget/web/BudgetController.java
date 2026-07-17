package com.nova.finance.budget.web;

import com.nova.auth.security.NovaUserPrincipal;
import com.nova.common.api.ApiResponse;
import com.nova.finance.budget.BudgetService;
import com.nova.finance.budget.web.dto.BudgetResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
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
}
