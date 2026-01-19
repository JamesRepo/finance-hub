package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.RecurrenceFrequency;
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
        @DisplayName("Should calculate total for monthly recurring income over full year")
        void shouldCalculateMonthlyRecurringIncomeForYear() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            IncomeSource monthlyIncome = createIncomeSource(
                    new BigDecimal("3000.00"),
                    new BigDecimal("2500.00"),
                    true,
                    RecurrenceFrequency.MONTHLY,
                    LocalDate.of(2024, 1, 1),
                    null
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(List.of(monthlyIncome));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            // 12 months * 2500 net = 30000
            assertEquals(new BigDecimal("30000.00"), result);
        }

        @Test
        @DisplayName("Should use gross amount when net amount is null")
        void shouldUseGrossAmountWhenNetIsNull() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            IncomeSource monthlyIncome = createIncomeSource(
                    new BigDecimal("3000.00"),
                    null,
                    true,
                    RecurrenceFrequency.MONTHLY,
                    LocalDate.of(2024, 1, 1),
                    null
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(List.of(monthlyIncome));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            // 12 months * 3000 gross = 36000
            assertEquals(new BigDecimal("36000.00"), result);
        }

        @Test
        @DisplayName("Should calculate weekly recurring income correctly")
        void shouldCalculateWeeklyRecurringIncome() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31);

            IncomeSource weeklyIncome = createIncomeSource(
                    new BigDecimal("500.00"),
                    null,
                    true,
                    RecurrenceFrequency.WEEKLY,
                    LocalDate.of(2024, 1, 1),
                    null
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(List.of(weeklyIncome));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            // Jan 1, 8, 15, 22, 29 = 5 weeks in January 2024
            assertEquals(new BigDecimal("2500.00"), result);
        }

        @Test
        @DisplayName("Should calculate bi-weekly recurring income correctly")
        void shouldCalculateBiWeeklyRecurringIncome() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31);

            IncomeSource biWeeklyIncome = createIncomeSource(
                    new BigDecimal("1500.00"),
                    null,
                    true,
                    RecurrenceFrequency.BI_WEEKLY,
                    LocalDate.of(2024, 1, 1),
                    null
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(List.of(biWeeklyIncome));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            // Jan 1, 15, 29 = 3 occurrences
            assertEquals(new BigDecimal("4500.00"), result);
        }

        @Test
        @DisplayName("Should calculate one-time income when within period")
        void shouldCalculateOneTimeIncomeWithinPeriod() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            IncomeSource oneTimeIncome = createIncomeSource(
                    new BigDecimal("5000.00"),
                    null,
                    false,
                    RecurrenceFrequency.ONE_TIME,
                    LocalDate.of(2024, 6, 15),
                    null
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(List.of(oneTimeIncome));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            assertEquals(new BigDecimal("5000.00"), result);
        }

        @Test
        @DisplayName("Should exclude one-time income when outside period")
        void shouldExcludeOneTimeIncomeOutsidePeriod() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            // Start date is before period start, so should not be counted
            IncomeSource oneTimeIncome = createIncomeSource(
                    new BigDecimal("5000.00"),
                    null,
                    false,
                    RecurrenceFrequency.ONE_TIME,
                    LocalDate.of(2023, 12, 15),
                    null
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(List.of(oneTimeIncome));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        @DisplayName("Should calculate quarterly recurring income correctly")
        void shouldCalculateQuarterlyRecurringIncome() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            IncomeSource quarterlyIncome = createIncomeSource(
                    new BigDecimal("2000.00"),
                    null,
                    true,
                    RecurrenceFrequency.QUARTERLY,
                    LocalDate.of(2024, 1, 1),
                    null
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(List.of(quarterlyIncome));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            // Jan, Apr, Jul, Oct = 4 quarters
            assertEquals(new BigDecimal("8000.00"), result);
        }

        @Test
        @DisplayName("Should calculate annual recurring income correctly")
        void shouldCalculateAnnualRecurringIncome() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            IncomeSource annualIncome = createIncomeSource(
                    new BigDecimal("10000.00"),
                    null,
                    true,
                    RecurrenceFrequency.ANNUALLY,
                    LocalDate.of(2024, 3, 15),
                    null
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(List.of(annualIncome));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            // 1 annual occurrence
            assertEquals(new BigDecimal("10000.00"), result);
        }

        @Test
        @DisplayName("Should respect end date of income source")
        void shouldRespectIncomeSourceEndDate() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            // Income ends in June
            IncomeSource monthlyIncome = createIncomeSource(
                    new BigDecimal("2000.00"),
                    null,
                    true,
                    RecurrenceFrequency.MONTHLY,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 6, 30)
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(List.of(monthlyIncome));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            // 6 months * 2000 = 12000
            assertEquals(new BigDecimal("12000.00"), result);
        }

        @Test
        @DisplayName("Should handle income starting mid-period")
        void shouldHandleIncomeStartingMidPeriod() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            // Income starts in July
            IncomeSource monthlyIncome = createIncomeSource(
                    new BigDecimal("3000.00"),
                    null,
                    true,
                    RecurrenceFrequency.MONTHLY,
                    LocalDate.of(2024, 7, 1),
                    null
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(List.of(monthlyIncome));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            // Jul, Aug, Sep, Oct, Nov, Dec = 6 months * 3000 = 18000
            assertEquals(new BigDecimal("18000.00"), result);
        }

        @Test
        @DisplayName("Should sum multiple income sources")
        void shouldSumMultipleIncomeSources() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            IncomeSource salary = createIncomeSource(
                    new BigDecimal("4000.00"),
                    new BigDecimal("3200.00"),
                    true,
                    RecurrenceFrequency.MONTHLY,
                    LocalDate.of(2024, 1, 1),
                    null
            );

            IncomeSource bonus = createIncomeSource(
                    new BigDecimal("5000.00"),
                    null,
                    false,
                    RecurrenceFrequency.ONE_TIME,
                    LocalDate.of(2024, 6, 1),
                    null
            );

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(Arrays.asList(salary, bonus));

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            // (12 * 3200) + 5000 = 38400 + 5000 = 43400
            assertEquals(new BigDecimal("43400.00"), result);
        }

        @Test
        @DisplayName("Should return zero when no income sources")
        void shouldReturnZeroWhenNoIncomeSources() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            when(incomeSourceRepository.findActiveInPeriod(testUser, startDate, endDate))
                    .thenReturn(Collections.emptyList());

            BigDecimal result = incomeSourceService.calculateTotalForPeriod(testUser, startDate, endDate);

            assertEquals(BigDecimal.ZERO, result);
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
            boolean isRecurring,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            LocalDate endDate
    ) {
        IncomeSource source = new IncomeSource();
        source.setUser(testUser);
        source.setGrossAmount(grossAmount);
        source.setNetAmount(netAmount);
        source.setIsRecurring(isRecurring);
        source.setRecurrenceFrequency(frequency);
        source.setStartDate(startDate);
        source.setEndDate(endDate);
        source.setIsActive(true);
        return source;
    }
}
