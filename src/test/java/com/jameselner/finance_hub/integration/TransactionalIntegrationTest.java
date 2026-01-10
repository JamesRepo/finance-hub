package com.jameselner.finance_hub.integration;

import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for transactional integration tests.
 * Each test method runs in a transaction that is rolled back after the test,
 * ensuring test isolation without manual cleanup.
 */
@Transactional
public abstract class TransactionalIntegrationTest extends BaseIntegrationTest {
    // All tests extending this class will automatically rollback after each test
}
