package com.nova.finance.goal.web;

import com.nova.auth.security.NovaUserPrincipal;
import com.nova.common.api.ApiResponse;
import com.nova.finance.goal.GoalService;
import com.nova.finance.goal.web.dto.AddGoalContributionRequest;
import com.nova.finance.goal.web.dto.CreateGoalRequest;
import com.nova.finance.goal.web.dto.GoalDetailResponse;
import com.nova.finance.goal.web.dto.GoalResponse;
import com.nova.finance.goal.web.dto.GoalSummaryResponse;
import com.nova.finance.goal.web.dto.UpdateGoalRequest;
import com.nova.user.UserRepository;
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
@RequestMapping("/api/goals")
@SecurityRequirement(name = "bearerAuth")
public class GoalController {

    private final GoalService goalService;
    private final UserRepository userRepository;

    public GoalController(GoalService goalService, UserRepository userRepository) {
        this.goalService = goalService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<List<GoalResponse>> listGoals(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @RequestParam(required = false) Boolean active) {
        return ApiResponse.ok(goalService.listGoals(principal.getUserId(), active));
    }

    @PostMapping
    public ApiResponse<GoalResponse> createGoal(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @Valid @RequestBody CreateGoalRequest request) {
        return ApiResponse.ok("Goal created.", goalService.createGoal(principal.getUserId(), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<GoalDetailResponse> getGoal(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @PathVariable UUID id) {
        return ApiResponse.ok(goalService.getGoal(principal.getUserId(), id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<GoalResponse> updateGoal(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGoalRequest request) {
        return ApiResponse.ok("Goal updated.", goalService.updateGoal(principal.getUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGoal(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @PathVariable UUID id) {
        goalService.deleteGoal(principal.getUserId(), id);
        return ApiResponse.ok("Goal deleted.");
    }

    @PostMapping("/{id}/contributions")
    public ApiResponse<GoalDetailResponse> addContribution(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AddGoalContributionRequest request) {
        return ApiResponse.ok("Contribution added.", goalService.addContribution(principal.getUserId(), id, request));
    }

    @GetMapping("/summary")
    public ApiResponse<GoalSummaryResponse> goalSummary(
            @AuthenticationPrincipal NovaUserPrincipal principal) {
        String currency = userRepository.findPreferredCurrencyById(principal.getUserId());
        return ApiResponse.ok(goalService.summary(principal.getUserId(), currency));
    }
}
