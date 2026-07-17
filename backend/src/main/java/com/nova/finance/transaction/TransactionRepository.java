package com.nova.finance.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByUserIdOrderByOccurredAtDesc(UUID userId);

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    List<Transaction> findByUserIdAndOccurredAtGreaterThanOrderByOccurredAtDesc(UUID userId, OffsetDateTime from);

    List<Transaction> findTop8ByUserIdOrderByOccurredAtDesc(UUID userId);

    /** Signed sum of a transaction type within an inclusive-start, exclusive-end window. */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.type = :type
              AND t.occurredAt >= :start
              AND t.occurredAt < :end
            """)
    BigDecimal sumAmount(@Param("userId") UUID userId,
                         @Param("type") Transaction.Type type,
                         @Param("start") OffsetDateTime start,
                         @Param("end") OffsetDateTime end);

    /**
     * Expense totals grouped by category for the dashboard "spending by category"
     * panel. Returns rows of { categoryName, color, icon, total }.
     */
    @Query("""
            SELECT c.name, c.color, c.icon, SUM(t.amount)
            FROM Transaction t
            JOIN t.category c
            WHERE t.user.id = :userId
              AND t.type = :type
              AND t.occurredAt >= :start
              AND t.occurredAt < :end
            GROUP BY c.id, c.name, c.color, c.icon
            ORDER BY SUM(t.amount) DESC
            """)
    List<Object[]> sumByCategory(@Param("userId") UUID userId,
                                 @Param("type") Transaction.Type type,
                                 @Param("start") OffsetDateTime start,
                                 @Param("end") OffsetDateTime end);

    /** Used to guard category deletion: a category still attached to transactions cannot be removed. */
    long countByCategoryId(UUID categoryId);

    /**
     * Raw expense rows within a window, for in-memory budget aggregation. Returns
     * rows of { categoryId, amount, occurredAt } so the caller can bucket a single
     * load across many budgets (each with its own sub-window and category scope) without
     * issuing a query per budget. Only {@code EXPENSE} rows are returned — income is
     * never counted as spending.
     */
    @Query("""
            SELECT t.category.id, t.amount, t.occurredAt
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.type = com.nova.finance.transaction.Transaction.Type.EXPENSE
              AND t.occurredAt >= :start
              AND t.occurredAt < :end
            """)
    List<Object[]> findExpenseRows(@Param("userId") UUID userId,
                                    @Param("start") OffsetDateTime start,
                                    @Param("end") OffsetDateTime end);
}
