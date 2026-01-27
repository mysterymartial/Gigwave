package com.gigwave;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests using embedded MongoDB.
 * All tests extending this class will use a real embedded MongoDB instance.
 * 
 * Embedded MongoDB will automatically start on a random port.
 * The test profile configuration handles the MongoDB setup.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class TestBase {
    // Base class for integration tests with embedded MongoDB
}
