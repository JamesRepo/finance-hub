package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.SavingsGoal;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.Priority;
import com.jameselner.finance_hub.dto.SavingsGoalDTO;
import com.jameselner.finance_hub.dto.SavingsGoalProgressDTO;
import com.jameselner.finance_hub.mapper.SavingsGoalMapper;
import com.jameselner.finance_hub.repository.SavingsGoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SavingsGoalService Tests")
class SavingsGoalServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @Mock
    private SavingsGoalMapper savingsGoalMapper;

    @InjectMocks
    private SavingsGoalService savingsGoalService;

    private SavingsGoalDTO validDto;
    private SavingsGoal validGoal;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);

        validDto = SavingsGoalDTO.builder()
                .userId(1L)
                .userName("testuser@example.com")
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("10000.00"))
                .currentAmount(new BigDecimal("5000.00"))
                .targetDate(LocalDate.now().plusYears(1))
                .priority(Priority.HIGH)
                .build();

        validGoal = new SavingsGoal();
        validGoal.setGoalId(1L);
        validGoal.setUser(testUser);
        validGoal.setGoalName("Emergency Fund");
        validGoal.setTargetAmount(new BigDecimal("10000.00"));
        validGoal.setCurrentAmount(new BigDecimal("5000.00"));
        validGoal.setTargetDate(LocalDate.now().plusYears(1));
        validGoal.setPriority(Priority.HIGH);
        validGoal.setCreatedAt(OffsetDateTime.now().minusMonths(6));
    }

    @Nested
    @DisplayName("createSavingsGoalFromDto Tests")
    class CreateSavingsGoalFromDtoTests {

        @Test
        @DisplayName("Should successfully create savings goal from valid DTO")
        void shouldCreateSavingsGoalSuccessfully() {
            when(savingsGoalMapper.toEntity(validDto)).thenReturn(validGoal);
            when(savingsGoalRepository.save(validGoal)).thenReturn(validGoal);
            when(savingsGoalMapper.toDto(validGoal)).thenReturn(validDto);

            SavingsGoalDTO result = savingsGoalService.createSavingsGoalFromDto(validDto);

            assertNotNull(result);
            verify(savingsGoalMapper).toEntity(validDto);
            verify(savingsGoalRepository).save(validGoal);
            verify(savingsGoalMapper).toDto(validGoal);
        }

        @Test
        @DisplayName("Should throw exception when DTO is null")
        void shouldThrowExceptionWhenDtoIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.createSavingsGoalFromDto(null)
            );
            assertEquals("SavingsGoalDTO must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when DTO has goal ID set")
        void shouldThrowExceptionWhenDtoHasId() {
            validDto.setGoalId(1L);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.createSavingsGoalFromDto(validDto)
            );
            assertEquals("Goal ID must be null for create operation", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when user ID is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            validDto.setUserId(null);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.createSavingsGoalFromDto(validDto)
            );
            assertEquals("User ID must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when goal name is null")
        void shouldThrowExceptionWhenGoalNameIsNull() {
            validDto.setGoalName(null);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.createSavingsGoalFromDto(validDto)
            );
            assertEquals("Goal name must not be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when target amount is null")
        void shouldThrowExceptionWhenTargetAmountIsNull() {
            validDto.setTargetAmount(null);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.createSavingsGoalFromDto(validDto)
            );
            assertEquals("Target amount must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when target amount is zero")
        void shouldThrowExceptionWhenTargetAmountIsZero() {
            validDto.setTargetAmount(BigDecimal.ZERO);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.createSavingsGoalFromDto(validDto)
            );
            assertEquals("Target amount must be greater than zero", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when current amount is negative")
        void shouldThrowExceptionWhenCurrentAmountIsNegative() {
            validDto.setCurrentAmount(new BigDecimal("-100.00"));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.createSavingsGoalFromDto(validDto)
            );
            assertEquals("Current amount must not be negative", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("updateSavingsGoalFromDto Tests")
    class UpdateSavingsGoalFromDtoTests {

        @Test
        @DisplayName("Should successfully update savings goal from valid DTO")
        void shouldUpdateSavingsGoalSuccessfully() {
            validDto.setGoalId(1L);

            when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(validGoal));
            doNothing().when(savingsGoalMapper).updateEntityFromDto(validGoal, validDto);
            when(savingsGoalRepository.save(validGoal)).thenReturn(validGoal);
            when(savingsGoalMapper.toDto(validGoal)).thenReturn(validDto);

            SavingsGoalDTO result = savingsGoalService.updateSavingsGoalFromDto(validDto);

            assertNotNull(result);
            verify(savingsGoalRepository).findById(1L);
            verify(savingsGoalMapper).updateEntityFromDto(validGoal, validDto);
            verify(savingsGoalRepository).save(validGoal);
            verify(savingsGoalMapper).toDto(validGoal);
        }

        @Test
        @DisplayName("Should throw exception when DTO is null")
        void shouldThrowExceptionWhenDtoIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.updateSavingsGoalFromDto(null)
            );
            assertEquals("SavingsGoalDTO must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when DTO has no goal ID")
        void shouldThrowExceptionWhenDtoHasNoId() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.updateSavingsGoalFromDto(validDto)
            );
            assertEquals("Goal ID must not be null for update operation", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when goal does not exist")
        void shouldThrowExceptionWhenGoalNotFound() {
            validDto.setGoalId(999L);
            when(savingsGoalRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.updateSavingsGoalFromDto(validDto)
            );
            assertEquals("Savings goal with ID 999 does not exist", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("findByIdAsDto Tests")
    class FindByIdAsDtoTests {

        @Test
        @DisplayName("Should successfully find savings goal by ID")
        void shouldFindSavingsGoalById() {
            when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(validGoal));
            when(savingsGoalMapper.toDto(validGoal)).thenReturn(validDto);

            Optional<SavingsGoalDTO> result = savingsGoalService.findByIdAsDto(1L);

            assertTrue(result.isPresent());
            assertEquals(validDto, result.get());
            verify(savingsGoalRepository).findById(1L);
            verify(savingsGoalMapper).toDto(validGoal);
        }

        @Test
        @DisplayName("Should return empty optional when goal not found")
        void shouldReturnEmptyWhenNotFound() {
            when(savingsGoalRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<SavingsGoalDTO> result = savingsGoalService.findByIdAsDto(999L);

            assertFalse(result.isPresent());
            verify(savingsGoalRepository).findById(999L);
            verify(savingsGoalMapper, never()).toDto(any());
        }

        @Test
        @DisplayName("Should throw exception when ID is null")
        void shouldThrowExceptionWhenIdIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.findByIdAsDto(null)
            );
            assertEquals("Savings goal ID must not be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("findAllAsDto Tests")
    class FindAllAsDtoTests {

        @Test
        @DisplayName("Should successfully find all savings goals")
        void shouldFindAllSavingsGoals() {
            SavingsGoal goal2 = new SavingsGoal();
            goal2.setGoalId(2L);
            List<SavingsGoal> goals = Arrays.asList(validGoal, goal2);

            SavingsGoalDTO dto2 = SavingsGoalDTO.builder()
                    .goalId(2L)
                    .goalName("Vacation Fund")
                    .build();

            when(savingsGoalRepository.findAll()).thenReturn(goals);
            when(savingsGoalMapper.toDto(validGoal)).thenReturn(validDto);
            when(savingsGoalMapper.toDto(goal2)).thenReturn(dto2);

            List<SavingsGoalDTO> result = savingsGoalService.findAllAsDto();

            assertEquals(2, result.size());
            verify(savingsGoalRepository).findAll();
            verify(savingsGoalMapper, times(2)).toDto(any(SavingsGoal.class));
        }

        @Test
        @DisplayName("Should return empty list when no goals exist")
        void shouldReturnEmptyListWhenNoGoals() {
            when(savingsGoalRepository.findAll()).thenReturn(Collections.emptyList());

            List<SavingsGoalDTO> result = savingsGoalService.findAllAsDto();

            assertTrue(result.isEmpty());
            verify(savingsGoalRepository).findAll();
        }
    }

    @Nested
    @DisplayName("deleteById Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should successfully delete savings goal by ID")
        void shouldDeleteSavingsGoalById() {
            when(savingsGoalRepository.existsById(1L)).thenReturn(true);
            doNothing().when(savingsGoalRepository).deleteById(1L);

            assertDoesNotThrow(() -> savingsGoalService.deleteById(1L));

            verify(savingsGoalRepository).existsById(1L);
            verify(savingsGoalRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when ID is null")
        void shouldThrowExceptionWhenIdIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.deleteById(null)
            );
            assertEquals("Savings goal ID must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when goal does not exist")
        void shouldThrowExceptionWhenGoalNotFound() {
            when(savingsGoalRepository.existsById(999L)).thenReturn(false);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.deleteById(999L)
            );
            assertEquals("Savings goal with ID 999 does not exist", exception.getMessage());
            verify(savingsGoalRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("calculateProgress Tests")
    class CalculateProgressTests {

        @Test
        @DisplayName("Should calculate progress correctly for active goal")
        void shouldCalculateProgressCorrectly() {
            SavingsGoalProgressDTO progress = savingsGoalService.calculateProgress(validGoal);

            assertNotNull(progress);
            assertEquals(validGoal.getGoalId(), progress.getGoalId());
            assertEquals(validGoal.getGoalName(), progress.getGoalName());
            assertEquals(new BigDecimal("10000.00"), progress.getTargetAmount());
            assertEquals(new BigDecimal("5000.00"), progress.getCurrentAmount());
            assertEquals(new BigDecimal("5000.00"), progress.getRemainingAmount());
            assertTrue(progress.getPercentageComplete().compareTo(BigDecimal.ZERO) > 0);
            assertNotNull(progress.getRecommendedMonthlyContribution());
        }

        @Test
        @DisplayName("Should mark goal as completed when current amount exceeds target")
        void shouldMarkGoalAsCompleted() {
            validGoal.setCurrentAmount(new BigDecimal("10000.00"));

            SavingsGoalProgressDTO progress = savingsGoalService.calculateProgress(validGoal);

            assertEquals("COMPLETED", progress.getStatus());
        }

        @Test
        @DisplayName("Should calculate recommended contributions correctly")
        void shouldCalculateRecommendedContributions() {
            SavingsGoalProgressDTO progress = savingsGoalService.calculateProgress(validGoal);

            assertNotNull(progress.getRecommendedMonthlyContribution());
            assertNotNull(progress.getRecommendedWeeklyContribution());
            assertTrue(progress.getRecommendedMonthlyContribution().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(progress.getRecommendedWeeklyContribution().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should throw exception when goal is null")
        void shouldThrowExceptionWhenGoalIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.calculateProgress((SavingsGoal) null)
            );
            assertEquals("Savings goal must not be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("addContribution Tests")
    class AddContributionTests {

        @Test
        @DisplayName("Should add contribution to goal")
        void shouldAddContribution() {
            when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(validGoal));
            when(savingsGoalRepository.save(validGoal)).thenReturn(validGoal);

            BigDecimal originalAmount = validGoal.getCurrentAmount();
            BigDecimal contributionAmount = new BigDecimal("500.00");

            savingsGoalService.addContribution(1L, contributionAmount);

            verify(savingsGoalRepository).findById(1L);
            verify(savingsGoalRepository).save(validGoal);
            assertEquals(originalAmount.add(contributionAmount), validGoal.getCurrentAmount());
        }

        @Test
        @DisplayName("Should throw exception when goal ID is null")
        void shouldThrowExceptionWhenGoalIdIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.addContribution(null, new BigDecimal("500.00"))
            );
            assertEquals("Savings goal ID must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.addContribution(1L, null)
            );
            assertEquals("Contribution amount must be greater than zero", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when amount is zero")
        void shouldThrowExceptionWhenAmountIsZero() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.addContribution(1L, BigDecimal.ZERO)
            );
            assertEquals("Contribution amount must be greater than zero", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when goal does not exist")
        void shouldThrowExceptionWhenGoalNotFound() {
            when(savingsGoalRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.addContribution(999L, new BigDecimal("500.00"))
            );
            assertEquals("Savings goal with ID 999 does not exist", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getTotalTargetAmountByUser Tests")
    class GetTotalTargetAmountByUserTests {

        @Test
        @DisplayName("Should get total target amount for user")
        void shouldGetTotalTargetAmountForUser() {
            BigDecimal expectedTotal = new BigDecimal("25000.00");
            when(savingsGoalRepository.getTotalTargetAmountByUser(testUser)).thenReturn(expectedTotal);

            BigDecimal result = savingsGoalService.getTotalTargetAmountByUser(testUser);

            assertEquals(expectedTotal, result);
            verify(savingsGoalRepository).getTotalTargetAmountByUser(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.getTotalTargetAmountByUser(null)
            );
            assertEquals("User must not be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getTotalCurrentAmountByUser Tests")
    class GetTotalCurrentAmountByUserTests {

        @Test
        @DisplayName("Should get total current amount for user")
        void shouldGetTotalCurrentAmountForUser() {
            BigDecimal expectedTotal = new BigDecimal("15000.00");
            when(savingsGoalRepository.getTotalCurrentAmountByUser(testUser)).thenReturn(expectedTotal);

            BigDecimal result = savingsGoalService.getTotalCurrentAmountByUser(testUser);

            assertEquals(expectedTotal, result);
            verify(savingsGoalRepository).getTotalCurrentAmountByUser(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> savingsGoalService.getTotalCurrentAmountByUser(null)
            );
            assertEquals("User must not be null", exception.getMessage());
        }
    }
}
