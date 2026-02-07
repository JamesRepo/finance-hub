package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.mapper.HolidayMapper;
import com.jameselner.finance_hub.repository.HolidayExpenseRepository;
import com.jameselner.finance_hub.repository.HolidayRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HolidayService.calculateTotalForMonth Tests")
class HolidayServiceCalculateTotalForMonthTest {

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private HolidayExpenseRepository holidayExpenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HolidayMapper holidayMapper;

    @InjectMocks
    private HolidayService holidayService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should return sum for valid inputs")
    void shouldReturnSumForValidInputs() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        LocalDate month = LocalDate.of(2024, 6, 15);
        LocalDate expectedStart = LocalDate.of(2024, 6, 1);
        LocalDate expectedEnd = LocalDate.of(2024, 6, 30);

        when(holidayExpenseRepository.sumByUserAndHolidayStartDateRange(
                eq(testUser), eq(expectedStart), eq(expectedEnd)))
                .thenReturn(new BigDecimal("1500.00"));

        BigDecimal result = holidayService.calculateTotalForMonth(1L, month);

        assertEquals(new BigDecimal("1500.00"), result);
    }

    @Test
    @DisplayName("Should return zero when no expenses")
    void shouldReturnZeroWhenNoExpenses() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        LocalDate month = LocalDate.of(2024, 6, 1);
        LocalDate expectedStart = LocalDate.of(2024, 6, 1);
        LocalDate expectedEnd = LocalDate.of(2024, 6, 30);

        when(holidayExpenseRepository.sumByUserAndHolidayStartDateRange(
                eq(testUser), eq(expectedStart), eq(expectedEnd)))
                .thenReturn(BigDecimal.ZERO);

        BigDecimal result = holidayService.calculateTotalForMonth(1L, month);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Should throw on null userId")
    void shouldThrowOnNullUserId() {
        assertThrows(IllegalArgumentException.class,
                () -> holidayService.calculateTotalForMonth(null, LocalDate.of(2024, 6, 1)));
    }

    @Test
    @DisplayName("Should throw on null month")
    void shouldThrowOnNullMonth() {
        assertThrows(IllegalArgumentException.class,
                () -> holidayService.calculateTotalForMonth(1L, null));
    }

    @Test
    @DisplayName("Should handle month boundaries correctly for February")
    void shouldHandleFebruaryCorrectly() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        LocalDate month = LocalDate.of(2024, 2, 15);
        LocalDate expectedStart = LocalDate.of(2024, 2, 1);
        LocalDate expectedEnd = LocalDate.of(2024, 2, 29); // 2024 is a leap year

        when(holidayExpenseRepository.sumByUserAndHolidayStartDateRange(
                eq(testUser), eq(expectedStart), eq(expectedEnd)))
                .thenReturn(new BigDecimal("200.00"));

        BigDecimal result = holidayService.calculateTotalForMonth(1L, month);

        assertEquals(new BigDecimal("200.00"), result);
    }
}
