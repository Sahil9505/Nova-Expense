package com.nova.finance.receipt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link Receipt}. All queries are scoped to a user so one
 * user can never read another's receipts.
 */
@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    Optional<Receipt> findByIdAndUserId(UUID id, UUID userId);

    List<Receipt> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Receipt> findTop8ByUserIdOrderByCreatedAtDesc(UUID userId);
}
