package com.nova.finance.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Goal> findByUserIdAndActiveOrderByCreatedAtDesc(UUID userId, boolean active);

    Optional<Goal> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT COUNT(g) > 0 FROM Goal g WHERE g.user.id = :userId AND LOWER(g.name) = LOWER(:name)")
    boolean existsByUserIdAndNameIgnoreCase(@Param("userId") UUID userId, @Param("name") String name);

    @Query("SELECT COUNT(g) > 0 FROM Goal g WHERE g.user.id = :userId AND LOWER(g.name) = LOWER(:name) AND g.id <> :id")
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(
            @Param("userId") UUID userId, @Param("name") String name, @Param("id") UUID id);
}
