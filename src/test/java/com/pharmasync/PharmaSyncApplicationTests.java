package com.pharmasync;

import org.junit.jupiter.api.Test;

class PharmaSyncApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Fails if any bean fails to wire, migrations fail to apply, or a required
        // infrastructure connection (Postgres, Redis, Kafka) cannot be established.
    }
}
