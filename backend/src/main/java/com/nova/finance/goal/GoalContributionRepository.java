package com.nova.finance.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {

    List<GoalContribution> findByGoalIdAndUserIdOrderByContributedAtDesc(UUID goalId, UUID userId);

    long countByGoalIdAndUserId(UUID goalId, UUID userId);

    /**
     * Aggregated contribution stats for many goals in a single query: for each goal
     * id, the total contributed, how many contributions exist, the first and last
     * contribution dates. The service pairs these with the entities in memory so a
     * goals list never issues one query per goal (no N+1).
     *
     * @return rows of { goalId, total, count, firstDate, lastDate }
     */
    @Query("""
            SELECT c.goal.id, COALESCE(SUM(c.amount), 0), COUNT(c.id),
                   MIN(c.contributedAt), MAX(c.contributedAt)
            FROM GoalContribution c
            WHERE c.goal.id IN :goalIds
            GROUP BY c.goal.id
            """)
    List<Object[]> aggregateByGoalIds(@Param("goalIds") List<UUID> goalIds);
}
