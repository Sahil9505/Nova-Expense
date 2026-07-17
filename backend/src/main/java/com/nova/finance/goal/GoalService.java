package com.nova.finance.goal;

import com.nova.common.exception.BadRequestException;
import com.nova.common.exception.ConflictException;
import com.nova.common.exception.ResourceNotFoundException;
import com.nova.finance.goal.web.dto.AddGoalContributionRequest;
import com.nova.finance.goal.web.dto.CreateGoalRequest;
import com.nova.finance.goal.web.dto.GoalDetailResponse;
import com.nova.finance.goal.web.dto.GoalProgress;
import com.nova.finance.goal.web.dto.GoalResponse;
import com.nova.finance.goal.web.dto.GoalSummaryResponse;
import com.nova.finance.goal.web.dto.GoalWithProgress;
import com.nova.finance.goal.web.dto.UpdateGoalRequest;
import com.nova.user.User;
import com.nova.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Goal lifecycle, ownership, and contribution logic. Every operation is scoped to the
 * authenticated user; one user can never read or mutate another's goal. Goals are never
 * hard-deleted — deactivation flips {@code active} so history survives while the goal
 * leaves active views (mirroring budgets and accounts).
 *
 * <p>Contributions are the immutable history; a goal's {@code currentAmount} is the
 * maintained running total, updated in the same transaction as the contribution row so
 * the two can never drift. This is the same pattern Nova uses to keep account balances
 * in sync with transactions.</p>
 */
@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalContributionRepository contributionRepository;
    private final UserRepository userRepository;
    private final GoalMapper goalMapper;

    public GoalService(
            GoalRepository goalRepository,
            GoalContributionRepository contributionRepository,
            UserRepository userRepository,
            GoalMapper goalMapper) {
        this.goalRepository = goalRepository;
        this.contributionRepository = contributionRepository;
        this.userRepository = userRepository;
        this.goalMapper = goalMapper;
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> listGoals(UUID userId, Boolean active) {
        List<Goal> goals = (active == null)
                ? goalRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : goalRepository.findByUserIdAndActiveOrderByCreatedAtDesc(userId, active);
        Map<UUID, ContributionStats> stats = loadStats(goals);
        LocalDate today = LocalDate.now();
        return goals.stream()
                .map(goal -> goalMapper.toResponse(
                        goal,
                        buildProgress(goal, stats.getOrDefault(goal.getId(), ContributionStats.EMPTY), today)))
                .toList();
    }

    @Transactional(readOnly = true)
    public GoalDetailResponse getGoal(UUID userId, UUID id) {
        Goal goal = loadOwned(id, userId);
        ContributionStats stats = loadStats(List.of(goal)).getOrDefault(goal.getId(), ContributionStats.EMPTY);
        GoalProgress progress = buildProgress(goal, stats, LocalDate.now());
        List<GoalContribution> contributions =
                contributionRepository.findByGoalIdAndUserIdOrderByContributedAtDesc(id, userId);
        return new GoalDetailResponse(
                goalMapper.toResponse(goal, progress),
                contributions.stream().map(goalMapper::toContributionResponse).toList());
    }

    @Transactional
    public GoalResponse createGoal(UUID userId, CreateGoalRequest request) {
        User user = loadUser(userId);
        String name = request.name().trim();
        if (goalRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("You already have a goal named '" + name + "'.");
        }
        Goal goal = new Goal(user, name, request.type(), request.targetAmount(), request.targetDate());
        goal.setDescription(blankToNull(request.description()));
        goal.setCurrentAmount(request.currentAmount() != null ? request.currentAmount() : BigDecimal.ZERO);
        goal.setPaused(false);
        return goalMapper.toResponse(goalRepository.save(goal), buildProgress(goal, ContributionStats.EMPTY, LocalDate.now()));
    }

    @Transactional
    public GoalResponse updateGoal(UUID userId, UUID id, UpdateGoalRequest request) {
        Goal goal = loadOwned(id, userId);

        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            if (!name.equalsIgnoreCase(goal.getName())
                    && goalRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, id)) {
                throw new ConflictException("You already have a goal named '" + name + "'.");
            }
            goal.setName(name);
        }
        if (request.type() != null) {
            goal.setType(request.type());
        }
        if (request.targetAmount() != null) {
            goal.setTargetAmount(request.targetAmount());
        }
        if (request.description() != null) {
            goal.setDescription(blankToNull(request.description()));
        }
        if (request.targetDate() != null) {
            goal.setTargetDate(request.targetDate());
        }
        if (request.currentAmount() != null) {
            goal.setCurrentAmount(nonNegative(request.currentAmount(), "Current amount"));
        }
        if (request.paused() != null) {
            goal.setPaused(request.paused());
        }
        if (request.active() != null) {
            goal.setActive(request.active());
        }

        GoalProgress progress = buildProgress(goal, ContributionStats.EMPTY, LocalDate.now());
        return goalMapper.toResponse(goalRepository.save(goal), progress);
    }

    @Transactional
    public void deleteGoal(UUID userId, UUID id) {
        Goal goal = loadOwned(id, userId);
        // Keep contribution history intact: deactivate instead of hard-deleting.
        goal.setActive(false);
        goalRepository.save(goal);
    }

    @Transactional
    public GoalDetailResponse addContribution(UUID userId, UUID goalId, AddGoalContributionRequest request) {
        Goal goal = loadOwned(goalId, userId);
        if (!goal.isActive()) {
            throw new BadRequestException("This goal is inactive. Reactivate it before adding contributions.");
        }
        User user = goal.getUser();

        GoalContribution contribution = new GoalContribution();
        contribution.setGoal(goal);
        contribution.setUser(user);
        contribution.setAmount(request.amount());
        contribution.setNote(blankToNull(request.note()));
        contribution.setContributedAt(request.contributedAt() != null ? request.contributedAt() : LocalDate.now());
        contributionRepository.save(contribution);

        // Maintain the running total within the same transaction; never exceed the target.
        BigDecimal next = goal.getCurrentAmount().add(request.amount());
        if (goal.getTargetAmount() != null && next.compareTo(goal.getTargetAmount()) > 0) {
            next = goal.getTargetAmount();
        }
        goal.setCurrentAmount(next);

        Goal saved = goalRepository.save(goal);

        ContributionStats stats = ContributionStats.from(
                request.amount(), 1, contribution.getContributedAt(), contribution.getContributedAt());
        GoalProgress progress = buildProgress(saved, stats, LocalDate.now());
        List<GoalContribution> contributions =
                contributionRepository.findByGoalIdAndUserIdOrderByContributedAtDesc(goalId, userId);
        return new GoalDetailResponse(
                goalMapper.toResponse(saved, progress),
                contributions.stream().map(goalMapper::toContributionResponse).toList());
    }

    @Transactional(readOnly = true)
    public GoalSummaryResponse summary(UUID userId, String currency) {
        List<Goal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (goals.isEmpty()) {
            return new GoalSummaryResponse(0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, currency, List.of());
        }

        Map<UUID, ContributionStats> stats = loadStats(goals);
        LocalDate today = LocalDate.now();

        List<GoalWithProgress> details = new ArrayList<>();
        int totalGoals = goals.size();
        int achievedCount = 0;
        int activeCount = 0;
        int pausedCount = 0;
        int overdueCount = 0;
        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;

        for (Goal goal : goals) {
            GoalProgress progress =
                    buildProgress(goal, stats.getOrDefault(goal.getId(), ContributionStats.EMPTY), today);
            details.add(new GoalWithProgress(goalMapper.toResponse(goal, progress), progress));
            if (goal.isActive()) {
                activeCount++;
                totalTarget = totalTarget.add(goal.getTargetAmount());
                totalCurrent = totalCurrent.add(goal.getCurrentAmount());
            }
            switch (GoalStatus.valueOf(progress.status())) {
                case ACHIEVED -> achievedCount++;
                case PAUSED -> pausedCount++;
                case OVERDUE -> overdueCount++;
                default -> { /* IN_PROGRESS / NOT_STARTED contribute only to active counts */ }
            }
        }

        BigDecimal overallPercent = totalTarget.signum() > 0
                ? totalCurrent.divide(totalTarget, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal remaining = totalTarget.subtract(totalCurrent).max(BigDecimal.ZERO);

        return new GoalSummaryResponse(totalGoals, activeCount, achievedCount, pausedCount,
                overdueCount, totalTarget, totalCurrent, remaining, overallPercent, currency, details);
    }

    // -----------------------------------------------------------------------
    // Progress + stats helpers
    // -----------------------------------------------------------------------

    /** Builds the per-goal progress from the entity and its aggregated contribution stats. */
    private GoalProgress buildProgress(Goal goal, ContributionStats stats, LocalDate today) {
        GoalCalculation calc = GoalCalculator.evaluate(
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getType(),
                goal.isPaused(),
                goal.getTargetDate(),
                stats.firstDate(),
                stats.lastDate(),
                today);
        return new GoalProgress(
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                calc.remaining(),
                calc.percentageComplete(),
                calc.status().name(),
                calc.estimatedCompletionDate());
    }

    /** Loads contribution stats for all goals in one query, keyed by goal id. */
    private Map<UUID, ContributionStats> loadStats(List<Goal> goals) {
        Map<UUID, ContributionStats> result = new LinkedHashMap<>();
        if (goals.isEmpty()) {
            return result;
        }
        List<UUID> ids = goals.stream().map(Goal::getId).toList();
        for (Object[] row : contributionRepository.aggregateByGoalIds(ids)) {
            UUID goalId = (UUID) row[0];
            BigDecimal total = (BigDecimal) row[1];
            long count = ((Number) row[2]).longValue();
            LocalDate first = (LocalDate) row[3];
            LocalDate last = (LocalDate) row[4];
            result.put(goalId, new ContributionStats(total, count, first, last));
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Ownership + validation helpers
    // -----------------------------------------------------------------------

    private Goal loadOwned(UUID id, UUID userId) {
        return goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id.toString()));
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    }

    private BigDecimal nonNegative(BigDecimal value, String field) {
        if (value.signum() < 0) {
            throw new BadRequestException(field + " cannot be negative.");
        }
        return value;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /** Aggregated contribution stats for a single goal (total, count, first/last dates). */
    private record ContributionStats(BigDecimal total, long count, LocalDate firstDate, LocalDate lastDate) {
        static final ContributionStats EMPTY = new ContributionStats(BigDecimal.ZERO, 0, null, null);

        static ContributionStats from(BigDecimal total, long count, LocalDate first, LocalDate last) {
            return new ContributionStats(total, count, first, last);
        }
    }
}
