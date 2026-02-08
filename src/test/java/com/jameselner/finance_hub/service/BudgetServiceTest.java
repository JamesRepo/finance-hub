package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Budget;
import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.PeriodType;
import com.jameselner.finance_hub.dto.BudgetDTO;
import com.jameselner.finance_hub.mapper.BudgetMapper;
import com.jameselner.finance_hub.repository.BudgetRepository;
import com.jameselner.finance_hub.repository.CategoryRepository;
import com.jameselner.finance_hub.repository.TransactionRepository;
import com.jameselner.finance_hub.repository.UserRepository;
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
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService Tests")
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetMapper budgetMapper;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BudgetService budgetService;

    private BudgetDTO validDto;
    private Budget validBudget;
    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");

        testCategory = new Category();
        testCategory.setCategoryId(1L);
        testCategory.setCategoryName("Groceries");
        testCategory.setColorCode("#FF0000");

        validDto = BudgetDTO.builder()
                .userId(1L)
                .userEmail("test@example.com")
                .categoryId(1L)
                .categoryName("Groceries")
                .categoryColorCode("#FF0000")
                .amount(new BigDecimal("500.00"))
                .periodType(PeriodType.MONTHLY)
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 31))
                .build();

        validBudget = new Budget();
        validBudget.setBudgetId(1L);
        validBudget.setUser(testUser);
        validBudget.setCategory(testCategory);
        validBudget.setAmount(new BigDecimal("500.00"));
        validBudget.setPeriodType(PeriodType.MONTHLY);
        validBudget.setStartDate(LocalDate.of(2025, 1, 1));
        validBudget.setEndDate(LocalDate.of(2025, 1, 31));
    }

    @Nested
    @DisplayName("Create Budget Tests")
    class CreateBudgetTests {

        @Test
        @DisplayName("Should create budget successfully with valid data")
        void shouldCreateBudgetSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(budgetRepository.findOverlappingBudgets(any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(budgetMapper.toEntity(validDto)).thenReturn(validBudget);
            when(budgetRepository.save(validBudget)).thenReturn(validBudget);
            when(budgetMapper.toDto(validBudget)).thenReturn(validDto);
            when(transactionRepository.sumByUserAndCategoryAndTypeAndDateRange(
                    any(), any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            BudgetDTO result = budgetService.createBudgetFromDto(validDto);

            // Then
            assertNotNull(result);
            verify(budgetRepository).save(validBudget);
            verify(budgetMapper).toEntity(validDto);
        }

        @Test
        @DisplayName("Should throw exception when DTO is null")
        void shouldThrowExceptionWhenDtoIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.createBudgetFromDto(null));
        }

        @Test
        @DisplayName("Should throw exception when budget ID is not null for create")
        void shouldThrowExceptionWhenBudgetIdNotNull() {
            // Given
            validDto.setBudgetId(1L);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.createBudgetFromDto(validDto));
        }

        @Test
        @DisplayName("Should throw exception when user ID is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            // Given
            validDto.setUserId(null);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.createBudgetFromDto(validDto));
        }

        @Test
        @DisplayName("Should throw exception when category ID is null")
        void shouldThrowExceptionWhenCategoryIdIsNull() {
            // Given
            validDto.setCategoryId(null);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.createBudgetFromDto(validDto));
        }

        @Test
        @DisplayName("Should throw exception when amount is zero")
        void shouldThrowExceptionWhenAmountIsZero() {
            // Given
            validDto.setAmount(BigDecimal.ZERO);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.createBudgetFromDto(validDto));
        }

        @Test
        @DisplayName("Should throw exception when amount is negative")
        void shouldThrowExceptionWhenAmountIsNegative() {
            // Given
            validDto.setAmount(new BigDecimal("-100.00"));

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.createBudgetFromDto(validDto));
        }

        @Test
        @DisplayName("Should throw exception when start date is after end date")
        void shouldThrowExceptionWhenStartDateAfterEndDate() {
            // Given
            validDto.setStartDate(LocalDate.of(2025, 2, 1));
            validDto.setEndDate(LocalDate.of(2025, 1, 31));

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.createBudgetFromDto(validDto));
        }

        @Test
        @DisplayName("Should throw exception when overlapping budget exists")
        void shouldThrowExceptionWhenOverlappingBudgetExists() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(budgetRepository.findOverlappingBudgets(any(), any(), any(), any()))
                    .thenReturn(Arrays.asList(validBudget));

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.createBudgetFromDto(validDto));
        }
    }

    @Nested
    @DisplayName("Update Budget Tests")
    class UpdateBudgetTests {

        @Test
        @DisplayName("Should update budget successfully with valid data")
        void shouldUpdateBudgetSuccessfully() {
            // Given
            validDto.setBudgetId(1L);
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(validBudget));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(budgetRepository.findOverlappingBudgets(any(), any(), any(), any()))
                    .thenReturn(Arrays.asList(validBudget));
            when(budgetRepository.save(validBudget)).thenReturn(validBudget);
            when(budgetMapper.toDto(validBudget)).thenReturn(validDto);
            when(transactionRepository.sumByUserAndCategoryAndTypeAndDateRange(
                    any(), any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            BudgetDTO result = budgetService.updateBudgetFromDto(validDto);

            // Then
            assertNotNull(result);
            verify(budgetRepository).save(validBudget);
            verify(budgetMapper).updateEntityFromDto(validBudget, validDto);
        }

        @Test
        @DisplayName("Should throw exception when budget ID is null for update")
        void shouldThrowExceptionWhenBudgetIdNull() {
            // Given
            validDto.setBudgetId(null);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.updateBudgetFromDto(validDto));
        }

        @Test
        @DisplayName("Should throw exception when budget does not exist")
        void shouldThrowExceptionWhenBudgetDoesNotExist() {
            // Given
            validDto.setBudgetId(999L);
            when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.updateBudgetFromDto(validDto));
        }
    }

    @Nested
    @DisplayName("Find Budget Tests")
    class FindBudgetTests {

        @Test
        @DisplayName("Should find budget by ID successfully")
        void shouldFindBudgetByIdSuccessfully() {
            // Given
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(validBudget));
            when(budgetMapper.toDto(validBudget)).thenReturn(validDto);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(transactionRepository.sumByUserAndCategoryAndTypeAndDateRange(
                    any(), any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            Optional<BudgetDTO> result = budgetService.findByIdAsDto(1L);

            // Then
            assertTrue(result.isPresent());
            verify(budgetRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return empty when budget not found")
        void shouldReturnEmptyWhenBudgetNotFound() {
            // Given
            when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<BudgetDTO> result = budgetService.findByIdAsDto(999L);

            // Then
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should find budgets by user ID successfully")
        void shouldFindBudgetsByUserIdSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(budgetRepository.findByUserOrderByStartDateDesc(testUser))
                    .thenReturn(Arrays.asList(validBudget));
            when(budgetMapper.toDto(validBudget)).thenReturn(validDto);
            when(transactionRepository.sumByUserAndCategoryAndTypeAndDateRange(
                    any(), any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            List<BudgetDTO> result = budgetService.findByUserIdAsDto(1L);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(budgetRepository).findByUserOrderByStartDateDesc(testUser);
        }
    }

    @Nested
    @DisplayName("Delete Budget Tests")
    class DeleteBudgetTests {

        @Test
        @DisplayName("Should delete budget successfully")
        void shouldDeleteBudgetSuccessfully() {
            // Given
            when(budgetRepository.existsById(1L)).thenReturn(true);

            // When
            budgetService.deleteById(1L);

            // Then
            verify(budgetRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent budget")
        void shouldThrowExceptionWhenDeletingNonExistentBudget() {
            // Given
            when(budgetRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.deleteById(999L));
        }

        @Test
        @DisplayName("Should throw exception when budget ID is null")
        void shouldThrowExceptionWhenBudgetIdIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.deleteById(null));
        }
    }

    @Nested
    @DisplayName("Calculate Spent Amount Tests")
    class CalculateSpentAmountTests {

        @Test
        @DisplayName("Should calculate spent amount correctly with expenses")
        void shouldCalculateSpentAmountWithExpenses() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(transactionRepository.sumByUserAndCategoryAndTypeAndDateRange(
                    any(), any(), any(), any(), any())).thenReturn(new BigDecimal("125.00"));

            // When
            BigDecimal result = budgetService.calculateSpentAmount(
                    1L,
                    1L,
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 31)
            );

            // Then
            assertEquals(new BigDecimal("125.00"), result);
        }

        @Test
        @DisplayName("Should return zero when no expenses found")
        void shouldReturnZeroWhenNoExpensesFound() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(transactionRepository.sumByUserAndCategoryAndTypeAndDateRange(
                    any(), any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            BigDecimal result = budgetService.calculateSpentAmount(
                    1L,
                    1L,
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 31)
            );

            // Then
            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        @DisplayName("Should only sum expense transactions (SUM query filters by type)")
        void shouldOnlySumExpenseTransactions() {
            // Given - the SUM query filters by TransactionType.EXPENSE at the DB level
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(transactionRepository.sumByUserAndCategoryAndTypeAndDateRange(
                    any(), any(), any(), any(), any())).thenReturn(new BigDecimal("50.00"));

            // When
            BigDecimal result = budgetService.calculateSpentAmount(
                    1L,
                    1L,
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 31)
            );

            // Then
            assertEquals(new BigDecimal("50.00"), result);
        }

        @Test
        @DisplayName("Should throw exception when user ID is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.calculateSpentAmount(
                            null,
                            1L,
                            LocalDate.of(2025, 1, 1),
                            LocalDate.of(2025, 1, 31)
                    ));
        }

        @Test
        @DisplayName("Should throw exception when category ID is null")
        void shouldThrowExceptionWhenCategoryIdIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.calculateSpentAmount(
                            1L,
                            null,
                            LocalDate.of(2025, 1, 1),
                            LocalDate.of(2025, 1, 31)
                    ));
        }
    }

    @Nested
    @DisplayName("Get Earliest Available Month Tests")
    class GetEarliestAvailableMonthTests {

        @Test
        @DisplayName("Should return earliest transaction month when transactions exist")
        void shouldReturnEarliestTransactionMonth() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(transactionRepository.findEarliestTransactionDateByUser(testUser))
                    .thenReturn(Optional.of(LocalDate.of(2024, 3, 15)));

            // When
            YearMonth result = budgetService.getEarliestAvailableMonth(1L);

            // Then
            assertEquals(YearMonth.of(2024, 3), result);
        }

        @Test
        @DisplayName("Should fall back to 12 months ago when no transactions exist")
        void shouldFallBackTo12MonthsAgoWhenNoTransactions() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(transactionRepository.findEarliestTransactionDateByUser(testUser))
                    .thenReturn(Optional.empty());

            // When
            YearMonth result = budgetService.getEarliestAvailableMonth(1L);

            // Then
            assertEquals(YearMonth.now().minusMonths(12), result);
        }

        @Test
        @DisplayName("Should throw exception when user ID is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.getEarliestAvailableMonth(null));
        }
    }

}
