package com.nova.finance.budget;

import com.nova.common.exception.BadRequestException;
import com.nova.common.exception.ConflictException;
import com.nova.common.exception.ResourceNotFoundException;
import com.nova.finance.budget.web.dto.BudgetResponse;
import com.nova.finance.budget.web.dto.CreateBudgetRequest;
import com.nova.finance.budget.web.dto.UpdateBudgetRequest;
import com.nova.finance.category.Category;
import com.nova.finance.category.CategoryRepository;
import com.nova.user.User;
import com.nova.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Budget lifecycle, ownership, and validation rules. Every operation is scoped to the
 * authenticated user; one user can never read or mutate another's budget. Budgets are
 * never hard-deleted — deactivation flips {@code isActive} so history survives while
 * the budget leaves active views.
 */
@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetMapper budgetMapper;

    public BudgetService(
            BudgetRepository budgetRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            BudgetMapper budgetMapper) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.budgetMapper = budgetMapper;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> listBudgets(UUID userId, Boolean active) {
        List<Budget> budgets = (active == null)
                ? budgetRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : budgetRepository.findByUserIdAndActiveOrderByCreatedAtDesc(userId, active);
        return budgets.stream().map(budgetMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudget(UUID userId, UUID id) {
        return budgetMapper.toResponse(loadOwned(id, userId));
    }

    @Transactional
    public BudgetResponse createBudget(UUID userId, CreateBudgetRequest request) {
        User user = loadUser(userId);
        String name = request.name().trim();
        if (budgetRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("You already have a budget named '" + name + "'.");
        }
        Category category = request.categoryId() != null ? loadOwnedCategory(request.categoryId(), userId) : null;
        if (request.period() == Budget.Period.CUSTOM) {
            requireCustomDateRange(request.startDate(), request.endDate());
        }
        Budget budget = new Budget(user, name, request.amount(), request.period(), request.startDate());
        budget.setDescription(blankToNull(request.description()));
        budget.setCategory(category);
        budget.setEndDate(request.endDate());
        return budgetMapper.toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetResponse updateBudget(UUID userId, UUID id, UpdateBudgetRequest request) {
        Budget budget = loadOwned(id, userId);

        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            if (!name.equalsIgnoreCase(budget.getName())
                    && budgetRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, id)) {
                throw new ConflictException("You already have a budget named '" + name + "'.");
            }
            budget.setName(name);
        }
        if (request.amount() != null) {
            budget.setAmount(request.amount());
        }
        if (request.period() != null) {
            budget.setPeriod(request.period());
        }
        if (request.categoryId() != null) {
            budget.setCategory(loadOwnedCategory(request.categoryId(), userId));
        }
        if (request.description() != null) {
            budget.setDescription(blankToNull(request.description()));
        }
        if (request.startDate() != null) {
            budget.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            budget.setEndDate(request.endDate());
        }
        if (request.active() != null) {
            budget.setActive(request.active());
        }

        Budget.Period effectivePeriod = budget.getPeriod();
        LocalDate effectiveEnd = budget.getEndDate();
        if (effectivePeriod == Budget.Period.CUSTOM) {
            requireCustomDateRange(budget.getStartDate(), effectiveEnd);
        } else if (effectiveEnd != null && budget.getStartDate() != null
                && effectiveEnd.isBefore(budget.getStartDate())) {
            throw new BadRequestException("End date cannot be before the start date.");
        }

        return budgetMapper.toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(UUID userId, UUID id) {
        Budget budget = loadOwned(id, userId);
        // Keep financial history intact: deactivate instead of hard-deleting.
        budget.setActive(false);
        budgetRepository.save(budget);
    }

    /** A CUSTOM budget must span an explicit, well-ordered date range. */
    private void requireCustomDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BadRequestException("Custom budgets require both a start date and an end date.");
        }
        if (end.isBefore(start)) {
            throw new BadRequestException("End date cannot be before the start date.");
        }
    }

    private Budget loadOwned(UUID id, UUID userId) {
        return budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id.toString()));
    }

    private Category loadOwnedCategory(UUID id, UUID userId) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
