package com.nova.finance.budget;

import com.nova.common.BaseEntity;
import com.nova.finance.category.Category;
import com.nova.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A spending limit a user sets for themselves. A budget may be scoped to a single
 * {@link Category} or, when {@code category} is null, apply to the user's overall
 * spending. Budgets are never deleted outright — deactivation ({@code isActive=false})
 * preserves history while hiding the budget from active views.
 */
@Entity
@Table(name = "budgets")
@Getter
@Setter
@NoArgsConstructor
public class Budget extends BaseEntity {

    /** How often a budget's limit resets. {@code CUSTOM} spans an explicit date range. */
    public enum Period {
        WEEKLY, MONTHLY, YEARLY, CUSTOM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Period period;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    public Budget(User user, String name, BigDecimal amount, Period period, LocalDate startDate) {
        this.user = user;
        this.name = name;
        this.amount = amount;
        this.period = period;
        this.startDate = startDate;
        this.active = true;
    }
}
