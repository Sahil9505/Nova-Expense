package com.nova.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Projects just the preferred currency without loading the whole user aggregate. */
    @Query("SELECT u.preferredCurrency FROM User u WHERE u.id = :id")
    String findPreferredCurrencyById(@Param("id") UUID id);
}
