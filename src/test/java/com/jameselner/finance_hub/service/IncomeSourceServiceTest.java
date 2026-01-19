package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.mapper.IncomeSourceMapper;
import com.jameselner.finance_hub.repository.IncomeSourceRepository;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncomeSourceService Tests")
class IncomeSourceServiceTest {

    @Mock
    private IncomeSourceRepository incomeSourceRepository;

    @Mock
    private IncomeSourceMapper incomeSourceMapper;

    @InjectMocks
    private IncomeSourceService incomeSourceService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
    }

    @Nested
    @DisplayName("calculateTotalForPeriod Tests")
    class CalculateTotalForPeriodTests {

        @Test
        @DisplayName("Should sum income source when start date is within period")
        void shouldSumIncomeSourceWithinPeriod() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31);

            IncomeSource januarySalary = createIncomeSource(
                    new BigDecimal("3000.00"),
                    new BigDecimal("2500.00"),
                    LocalDate.of(2024, 1, 15)
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(List.of(januarySalary));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            assertEquals(new BigDecimal("2500.00"), result);
        }

        @Test
        @DisplayName("Should use gross amount when net amount is null")
        void shouldUseGrossAmountWhenNetIsNull() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31);

            IncomeSource januarySalary = createIncomeSource(
                    new BigDecimal("3000.00"),
                    null,
                    LocalDate.of(2024, 1, 15)
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(List.of(januarySalary));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            assertEquals(new BigDecimal("3000.00"), result);
        }

        @Test
        @DisplayName("Should sum multiple income sources in same month")
        void shouldSumMultipleIncomeSourcesInSameMonth() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31);

            IncomeSource salary = createIncomeSource(
                    new BigDecimal("4000.00"),
                    new BigDecimal("3200.00"),
                    LocalDate.of(2024, 1, 15)
            );

            IncomeSource bonus = createIncomeSource(
                    new BigDecimal("1000.00"),
                    null,
                    LocalDate.of(2024, 1, 20)
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(Arrays.asList(salary, bonus));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            // 3200 (net) + 1000 (gross) = 4200
            assertEquals(new BigDecimal("4200.00"), result);
        }

        @Test
        @DisplayName("Should return zero when no income sources in period")
        void shouldReturnZeroWhenNoIncomeSources() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31);

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(Collections.emptyList());

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        @DisplayName("Should calculate yearly total from monthly entries")
        void shouldCalculateYearlyTotalFromMonthlyEntries() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            // Simulate 12 monthly salary entries with slight variations
            List<IncomeSource> monthlySalaries = Arrays.asList(
                    createIncomeSource(new BigDecimal("3000.00"), new BigDecimal("2500.00"), LocalDate.of(2024, 1, 15)),
                    createIncomeSource(new BigDecimal("3000.00"), new BigDecimal("2500.00"), LocalDate.of(2024, 2, 15)),
                    createIncomeSource(new BigDecimal("3000.00"), new BigDecimal("2500.00"), LocalDate.of(2024, 3, 15)),
                    createIncomeSource(new BigDecimal("3000.00"), new BigDecimal("2500.00"), LocalDate.of(2024, 4, 15)),
                    createIncomeSource(new BigDecimal("3000.00"), new BigDecimal("2500.00"), LocalDate.of(2024, 5, 15)),
                    createIncomeSource(new BigDecimal("3000.00"), new BigDecimal("2500.00"), LocalDate.of(2024, 6, 15)),
                    createIncomeSource(new BigDecimal("3000.00"), new BigDecimal("2500.00"), LocalDate.of(2024, 7, 15)),
                    createIncomeSource(new BigDecimal("3000.00"), new BigDecimal("2500.00"), LocalDate.of(2024, 8, 15)),
                    createIncomeSource(new BigDecimal("3000.00"), new BigDecimal("2500.00"), LocalDate.of(2024, 9, 15)),
                    createIncomeSource(new BigDecimal("3000.00"), new BigDecimal("2500.00"), LocalDate.of(2024, 10, 15)),
                    createIncomeSource(new BigDecimal("3000.00"), new BigDecimal("2500.00"), LocalDate.of(2024, 11, 15)),
                    createIncomeSource(new BigDecimal("3000.00"), new BigDecimal("2500.00"), LocalDate.of(2024, 12, 15))
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(monthlySalaries);

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            // 12 * 2500 = 30000
            assertEquals(new BigDecimal("30000.00"), result);
        }

        @Test
        @DisplayName("Should only include income sources within queried month")
        void shouldOnlyIncludeIncomeSourcesWithinQueriedMonth() {
            // Query for January only
            LocalDate janStart = LocalDate.of(2024, 1, 1);
            LocalDate janEnd = LocalDate.of(2024, 1, 31);

            IncomeSource januarySalary = createIncomeSource(
                    new BigDecimal("3000.00"),
                    new BigDecimal("2500.00"),
                    LocalDate.of(2024, 1, 15)
            );

            // Repository returns only January entry (query handles the filtering)
            when(incomeSourceRepository.findActiveInPeriod(testUser, janStart, janEnd))
                    .thenReturn(List.of(januarySalary));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, janStart, janEnd);

            assertEquals(new BigDecimal("2500.00"), result);
        }

        @Test
        @DisplayName("Should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> incomeSourceService.calculateTotalForPeriod(null, startDate, endDate)
            );
            assertEquals("User must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when start date is null")
        void shouldThrowExceptionWhenStartDateIsNull() {
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> incomeSourceService.calculateTotalForPeriod(testUser, null, endDate)
            );
            assertEquals("Start date and end date must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when end date is null")
        void shouldThrowExceptionWhenEndDateIsNull() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> incomeSourceService.calculateTotalForPeriod(testUser, startDate, null)
            );
            assertEquals("Start date and end date must not be null", exception.getMessage());
        }
    }

    private IncomeSource createIncomeSource(
            BigDecimal grossAmount,
            BigDecimal netAmount,
            LocalDate startDate
    ) {
        IncomeSource source = new IncomeSource();
        source.setUser(testUser);
        source.setGrossAmount(grossAmount);
        source.setNetAmount(netAmount);
        source.setStartDate(startDate);
        source.setIsActive(true);
        return source;
    }
}
