package com.nova.finance.goal;

import com.nova.common.BaseEntity;
import com.nova.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * One logged addition toward a {@link Goal}. Contributions are immutable history —
 * the goal's {@code currentAmount} is the maintained running total, so reads never
 * recompute a sum. Each row carries its own amount, date, and optional note so the
 * progress timeline can be rendered directly from the records.
 */
@Entity
@Table(name = "goal_contributions")
@Getter
@Setter
@NoArgsConstructor
public class GoalContribution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(length = 255)
    private String note;

    @Column(name = "contributed_at", nullable = false)
    private LocalDate contributedAt;
}
