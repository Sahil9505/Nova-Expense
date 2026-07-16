package com.nova.health;

import java.time.OffsetDateTime;

/**
 * Represents the liveness of the Nova backend. Extend in later phases with
 * downstream dependency checks (database, cache, external services).
 */
public record HealthStatus(String status, String service, OffsetDateTime timestamp) {

    public static HealthStatus up() {
        return new HealthStatus("UP", "nova-backend", OffsetDateTime.now());
    }
}
