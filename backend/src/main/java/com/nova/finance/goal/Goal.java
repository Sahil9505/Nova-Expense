package com.nova.finance.goal;

import com.nova.common.BaseEntity;
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
 * A long-term financial objective a user is working toward. The {@code type}
 * distinguishes intent (savings, debt payoff, custom) but every goal is measured
 * the same way: a {@code targetAmount} approached by a maintained {@code currentAmount}.
 *
 * <p>Goals are never hard-deleted — deactivation ({@code active=false}) preserves
 * history and removes the goal from active views, mirroring budgets and accounts. A
 * goal can also be {@code paused} without losing it; pausing is a manual preference,
 * whereas the rest of a goal's {@link GoalStatus} is derived from its progress and
 * target date.</p>
 */
@Entity
@Table(name = "goals")
@Getter
@Setter
@NoArgsConstructor
public class Goal extends BaseEntity {

    /** The intent of the goal. Stored as a string so a new type is a data change, not a migration. */
    public enum Type {
        SAVINGS,
        DEBT_PAYOFF,
        CUSTOM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 32)
    private Type type;

    @Column(name = "target_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal currentAmount;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "paused", nullable = false)
    private boolean paused;

    public Goal(User user, String name, Type type, BigDecimal targetAmount, LocalDate targetDate) {
        this.user = user;
        this.name = name;
        this.type = type;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
        this.currentAmount = BigDecimal.ZERO;
        this.active = true;
        this.paused = false;
    }
}
