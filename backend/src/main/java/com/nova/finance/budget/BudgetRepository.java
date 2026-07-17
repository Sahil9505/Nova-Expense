package com.nova.finance.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Budget> findByUserIdAndActiveOrderByCreatedAtDesc(UUID userId, boolean active);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT COUNT(b) > 0 FROM Budget b WHERE b.user.id = :userId AND LOWER(b.name) = LOWER(:name)")
    boolean existsByUserIdAndNameIgnoreCase(@Param("userId") UUID userId, @Param("name") String name);

    @Query("SELECT COUNT(b) > 0 FROM Budget b WHERE b.user.id = :userId AND LOWER(b.name) = LOWER(:name) AND b.id <> :id")
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(
            @Param("userId") UUID userId, @Param("name") String name, @Param("id") UUID id);
}
