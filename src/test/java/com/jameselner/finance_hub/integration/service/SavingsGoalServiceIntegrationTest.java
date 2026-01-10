package com.jameselner.finance_hub.integration.service;

import com.jameselner.finance_hub.domain.enums.Priority;
import com.jameselner.finance_hub.dto.SavingsGoalDTO;
import com.jameselner.finance_hub.integration.TransactionalIntegrationTest;
import com.jameselner.finance_hub.integration.fixtures.TestFixtureFactory;
import com.jameselner.finance_hub.integration.fixtures.TestFixtureFactory.TestUserContext;
import com.jameselner.finance_hub.repository.SavingsGoalRepository;
import com.jameselner.finance_hub.service.SavingsGoalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SavingsGoalService Integration Tests")
class SavingsGoalServiceIntegrationTest extends TransactionalIntegrationTest {

    @Autowired
    private SavingsGoalService savingsGoalService;

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private TestFixtureFactory fixtureFactory;

    private TestUserContext userContext;

    @BeforeEach
    void setUp() {
        userContext = fixtureFactory.createBasicUser("savings-test@example.com");
    }

    @Nested
    @DisplayName("Savings Goal CRUD")
    class CrudTests {

        @Test
        @DisplayName("Should create savings goal")
        void shouldCreateSavingsGoal() {
            SavingsGoalDTO dto = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("Emergency Fund")
                    .targetAmount(new BigDecimal("10000.00"))
                    .currentAmount(BigDecimal.ZERO)
                    .targetDate(LocalDate.now().plusMonths(12))
                    .priority(Priority.HIGH)
                    .build();

            SavingsGoalDTO created = savingsGoalService.createSavingsGoalFromDto(dto);

            assertNotNull(created.getGoalId());
            assertEquals("Emergency Fund", created.getGoalName());
            assertEquals(new BigDecimal("10000.00"), created.getTargetAmount());
            assertEquals(Priority.HIGH, created.getPriority());
        }

        @Test
        @DisplayName("Should update savings goal")
        void shouldUpdateSavingsGoal() {
            SavingsGoalDTO dto = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("Original Goal")
                    .targetAmount(new BigDecimal("5000.00"))
                    .currentAmount(BigDecimal.ZERO)
                    .targetDate(LocalDate.now().plusMonths(6))
                    .priority(Priority.MEDIUM)
                    .build();
            SavingsGoalDTO created = savingsGoalService.createSavingsGoalFromDto(dto);

            created.setGoalName("Updated Goal");
            created.setTargetAmount(new BigDecimal("7500.00"));
            SavingsGoalDTO updated = savingsGoalService.updateSavingsGoalFromDto(created);

            assertEquals("Updated Goal", updated.getGoalName());
            assertEquals(new BigDecimal("7500.00"), updated.getTargetAmount());
        }

        @Test
        @DisplayName("Should delete savings goal")
        void shouldDeleteSavingsGoal() {
            SavingsGoalDTO dto = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("To Delete")
                    .targetAmount(new BigDecimal("1000.00"))
                    .currentAmount(BigDecimal.ZERO)
                    .targetDate(LocalDate.now().plusMonths(3))
                    .priority(Priority.LOW)
                    .build();
            SavingsGoalDTO created = savingsGoalService.createSavingsGoalFromDto(dto);

            savingsGoalService.deleteById(created.getGoalId());

            Optional<SavingsGoalDTO> found = savingsGoalService.findByIdAsDto(created.getGoalId());
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should add contribution to goal")
        void shouldAddContribution() {
            SavingsGoalDTO dto = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("Vacation Fund")
                    .targetAmount(new BigDecimal("5000.00"))
                    .currentAmount(new BigDecimal("1000.00"))
                    .targetDate(LocalDate.now().plusMonths(6))
                    .priority(Priority.MEDIUM)
                    .build();
            SavingsGoalDTO created = savingsGoalService.createSavingsGoalFromDto(dto);

            savingsGoalService.addContribution(created.getGoalId(), new BigDecimal("500.00"));

            SavingsGoalDTO updated = savingsGoalService.findByIdAsDto(created.getGoalId()).orElseThrow();
            assertEquals(new BigDecimal("1500.00"), updated.getCurrentAmount());
        }

        @Test
        @DisplayName("Should add multiple contributions")
        void shouldAddMultipleContributions() {
            SavingsGoalDTO dto = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("House Deposit")
                    .targetAmount(new BigDecimal("20000.00"))
                    .currentAmount(BigDecimal.ZERO)
                    .targetDate(LocalDate.now().plusYears(2))
                    .priority(Priority.HIGH)
                    .build();
            SavingsGoalDTO created = savingsGoalService.createSavingsGoalFromDto(dto);

            savingsGoalService.addContribution(created.getGoalId(), new BigDecimal("1000.00"));
            savingsGoalService.addContribution(created.getGoalId(), new BigDecimal("500.00"));
            savingsGoalService.addContribution(created.getGoalId(), new BigDecimal("750.00"));

            SavingsGoalDTO updated = savingsGoalService.findByIdAsDto(created.getGoalId()).orElseThrow();
            assertEquals(new BigDecimal("2250.00"), updated.getCurrentAmount());
        }
    }

    @Nested
    @DisplayName("Savings Goal Queries")
    class QueryTests {

        @Test
        @DisplayName("Should find all goals by user")
        void shouldFindAllGoalsByUser() {
            for (int i = 0; i < 3; i++) {
                SavingsGoalDTO dto = SavingsGoalDTO.builder()
                        .userId(userContext.user().getUserId())
                        .goalName("Goal " + i)
                        .targetAmount(new BigDecimal("1000.00"))
                        .currentAmount(BigDecimal.ZERO)
                        .targetDate(LocalDate.now().plusMonths(i + 1))
                        .priority(Priority.MEDIUM)
                        .build();
                savingsGoalService.createSavingsGoalFromDto(dto);
            }

            List<SavingsGoalDTO> goals = savingsGoalService.findByUserAsDto(userContext.user());

            assertEquals(3, goals.size());
        }

        @Test
        @DisplayName("Should find active goals (not yet completed)")
        void shouldFindActiveGoals() {
            // Active goal (current < target)
            SavingsGoalDTO active = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("Active Goal")
                    .targetAmount(new BigDecimal("5000.00"))
                    .currentAmount(new BigDecimal("1000.00"))
                    .targetDate(LocalDate.now().plusMonths(6))
                    .priority(Priority.HIGH)
                    .build();
            savingsGoalService.createSavingsGoalFromDto(active);

            // Completed goal (current >= target)
            SavingsGoalDTO completed = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("Completed Goal")
                    .targetAmount(new BigDecimal("1000.00"))
                    .currentAmount(new BigDecimal("1000.00"))
                    .targetDate(LocalDate.now().plusMonths(1))
                    .priority(Priority.LOW)
                    .build();
            savingsGoalService.createSavingsGoalFromDto(completed);

            List<SavingsGoalDTO> activeGoals = savingsGoalService.findActiveGoalsByUserAsDto(userContext.user());

            assertEquals(1, activeGoals.size());
            assertEquals("Active Goal", activeGoals.getFirst().getGoalName());
        }

        @Test
        @DisplayName("Should find completed goals")
        void shouldFindCompletedGoals() {
            // Active goal
            SavingsGoalDTO active = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("Active Goal")
                    .targetAmount(new BigDecimal("5000.00"))
                    .currentAmount(new BigDecimal("1000.00"))
                    .targetDate(LocalDate.now().plusMonths(6))
                    .priority(Priority.HIGH)
                    .build();
            savingsGoalService.createSavingsGoalFromDto(active);

            // Completed goal
            SavingsGoalDTO completed = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("Completed Goal")
                    .targetAmount(new BigDecimal("1000.00"))
                    .currentAmount(new BigDecimal("1500.00"))
                    .targetDate(LocalDate.now().plusMonths(1))
                    .priority(Priority.LOW)
                    .build();
            savingsGoalService.createSavingsGoalFromDto(completed);

            List<SavingsGoalDTO> completedGoals = savingsGoalService.findCompletedGoalsByUserAsDto(userContext.user());

            assertEquals(1, completedGoals.size());
            assertEquals("Completed Goal", completedGoals.getFirst().getGoalName());
        }

        @Test
        @DisplayName("Should find goals by priority")
        void shouldFindGoalsByPriority() {
            SavingsGoalDTO high = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("High Priority")
                    .targetAmount(new BigDecimal("10000.00"))
                    .currentAmount(BigDecimal.ZERO)
                    .targetDate(LocalDate.now().plusMonths(12))
                    .priority(Priority.HIGH)
                    .build();
            savingsGoalService.createSavingsGoalFromDto(high);

            SavingsGoalDTO low = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("Low Priority")
                    .targetAmount(new BigDecimal("1000.00"))
                    .currentAmount(BigDecimal.ZERO)
                    .targetDate(LocalDate.now().plusMonths(6))
                    .priority(Priority.LOW)
                    .build();
            savingsGoalService.createSavingsGoalFromDto(low);

            List<SavingsGoalDTO> highPriority = savingsGoalService.findByUserAndPriorityAsDto(
                    userContext.user(), Priority.HIGH);

            assertEquals(1, highPriority.size());
            assertEquals("High Priority", highPriority.getFirst().getGoalName());
        }
    }

    @Nested
    @DisplayName("Progress Calculations")
    class ProgressCalculationTests {

        @Test
        @DisplayName("Should calculate total target amount by user")
        void shouldCalculateTotalTargetAmount() {
            SavingsGoalDTO goal1 = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("Goal 1")
                    .targetAmount(new BigDecimal("5000.00"))
                    .currentAmount(BigDecimal.ZERO)
                    .targetDate(LocalDate.now().plusMonths(6))
                    .priority(Priority.MEDIUM)
                    .build();
            savingsGoalService.createSavingsGoalFromDto(goal1);

            SavingsGoalDTO goal2 = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("Goal 2")
                    .targetAmount(new BigDecimal("3000.00"))
                    .currentAmount(BigDecimal.ZERO)
                    .targetDate(LocalDate.now().plusMonths(12))
                    .priority(Priority.HIGH)
                    .build();
            savingsGoalService.createSavingsGoalFromDto(goal2);

            BigDecimal total = savingsGoalService.getTotalTargetAmountByUser(userContext.user());

            assertEquals(new BigDecimal("8000.00"), total);
        }

        @Test
        @DisplayName("Should calculate total current amount by user")
        void shouldCalculateTotalCurrentAmount() {
            SavingsGoalDTO goal1 = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("Goal 1")
                    .targetAmount(new BigDecimal("5000.00"))
                    .currentAmount(new BigDecimal("1500.00"))
                    .targetDate(LocalDate.now().plusMonths(6))
                    .priority(Priority.MEDIUM)
                    .build();
            savingsGoalService.createSavingsGoalFromDto(goal1);

            SavingsGoalDTO goal2 = SavingsGoalDTO.builder()
                    .userId(userContext.user().getUserId())
                    .goalName("Goal 2")
                    .targetAmount(new BigDecimal("3000.00"))
                    .currentAmount(new BigDecimal("500.00"))
                    .targetDate(LocalDate.now().plusMonths(12))
                    .priority(Priority.HIGH)
                    .build();
            savingsGoalService.createSavingsGoalFromDto(goal2);

            BigDecimal total = savingsGoalService.getTotalCurrentAmountByUser(userContext.user());

            assertEquals(new BigDecimal("2000.00"), total);
        }
    }
}
