package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.DebtType;
import com.jameselner.finance_hub.dto.DebtDTO;
import com.jameselner.finance_hub.mapper.DebtMapper;
import com.jameselner.finance_hub.repository.DebtRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DebtService Tests")
class DebtServiceTest {

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private DebtMapper debtMapper;

    @InjectMocks
    private DebtService debtService;

    private DebtDTO validDto;
    private Debt validDebt;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);

        validDto = DebtDTO.builder()
                .userId(1L)
                .userName("testuser")
                .debtName("Credit Card")
                .debtType(DebtType.CREDIT_CARD)
                .principalAmount(new BigDecimal("10000.00"))
                .currentBalance(new BigDecimal("8000.00"))
                .interestRate(new BigDecimal("18.99"))
                .startDate(LocalDate.of(2023, 1, 1))
                .targetPayoffDate(LocalDate.of(2025, 12, 31))
                .minimumPayment(new BigDecimal("200.00"))
                .build();

        validDebt = new Debt();
        validDebt.setDebtId(1L);
        validDebt.setUser(testUser);
        validDebt.setDebtName("Credit Card");
        validDebt.setDebtType(DebtType.CREDIT_CARD);
        validDebt.setPrincipalAmount(new BigDecimal("10000.00"));
        validDebt.setCurrentBalance(new BigDecimal("8000.00"));
        validDebt.setInterestRate(new BigDecimal("18.99"));
        validDebt.setStartDate(LocalDate.of(2023, 1, 1));
        validDebt.setMinimumPayment(new BigDecimal("200.00"));
        validDebt.setDeleted(false);
    }

    @Nested
    @DisplayName("createDebtFromDto Tests")
    class CreateDebtFromDtoTests {

        @Test
        @DisplayName("Should successfully create debt from valid DTO")
        void shouldCreateDebtSuccessfully() {
            when(debtMapper.toEntity(validDto)).thenReturn(validDebt);
            when(debtRepository.save(validDebt)).thenReturn(validDebt);
            when(debtMapper.toDto(validDebt)).thenReturn(validDto);

            DebtDTO result = debtService.createDebtFromDto(validDto);

            assertNotNull(result);
            verify(debtMapper).toEntity(validDto);
            verify(debtRepository).save(validDebt);
            verify(debtMapper).toDto(validDebt);
        }

        @Test
        @DisplayName("Should throw exception when DTO is null")
        void shouldThrowExceptionWhenDtoIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.createDebtFromDto(null)
            );
            assertEquals("DebtDTO must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when DTO has debt ID set")
        void shouldThrowExceptionWhenDtoHasId() {
            validDto.setDebtId(1L);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.createDebtFromDto(validDto)
            );
            assertEquals("Debt ID must be null for create operation", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when user ID is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            validDto.setUserId(null);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.createDebtFromDto(validDto)
            );
            assertEquals("User ID must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when debt name is null")
        void shouldThrowExceptionWhenDebtNameIsNull() {
            validDto.setDebtName(null);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.createDebtFromDto(validDto)
            );
            assertEquals("Debt name must not be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when debt name is empty")
        void shouldThrowExceptionWhenDebtNameIsEmpty() {
            validDto.setDebtName("   ");

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.createDebtFromDto(validDto)
            );
            assertEquals("Debt name must not be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when debt type is null")
        void shouldThrowExceptionWhenDebtTypeIsNull() {
            validDto.setDebtType(null);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.createDebtFromDto(validDto)
            );
            assertEquals("Debt type must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should allow null principal amount (optional field)")
        void shouldAllowNullPrincipalAmount() {
            validDto.setPrincipalAmount(null);

            when(debtMapper.toEntity(validDto)).thenReturn(validDebt);
            when(debtRepository.save(validDebt)).thenReturn(validDebt);
            when(debtMapper.toDto(validDebt)).thenReturn(validDto);

            DebtDTO result = debtService.createDebtFromDto(validDto);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when principal amount is zero")
        void shouldThrowExceptionWhenPrincipalAmountIsZero() {
            validDto.setPrincipalAmount(BigDecimal.ZERO);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.createDebtFromDto(validDto)
            );
            assertEquals("Principal amount must be greater than zero", exception.getMessage());
        }

        @Test
        @DisplayName("Should allow null start date (optional field)")
        void shouldAllowNullStartDate() {
            validDto.setStartDate(null);

            when(debtMapper.toEntity(validDto)).thenReturn(validDebt);
            when(debtRepository.save(validDebt)).thenReturn(validDebt);
            when(debtMapper.toDto(validDebt)).thenReturn(validDto);

            DebtDTO result = debtService.createDebtFromDto(validDto);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when current balance is negative")
        void shouldThrowExceptionWhenCurrentBalanceIsNegative() {
            validDto.setCurrentBalance(new BigDecimal("-100.00"));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.createDebtFromDto(validDto)
            );
            assertEquals("Current balance must not be negative", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when interest rate is negative")
        void shouldThrowExceptionWhenInterestRateIsNegative() {
            validDto.setInterestRate(new BigDecimal("-5.00"));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.createDebtFromDto(validDto)
            );
            assertEquals("Interest rate must not be negative", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("updateDebtFromDto Tests")
    class UpdateDebtFromDtoTests {

        @Test
        @DisplayName("Should successfully update debt from valid DTO")
        void shouldUpdateDebtSuccessfully() {
            validDto.setDebtId(1L);

            when(debtRepository.findById(1L)).thenReturn(Optional.of(validDebt));
            doNothing().when(debtMapper).updateEntityFromDto(validDebt, validDto);
            when(debtRepository.save(validDebt)).thenReturn(validDebt);
            when(debtMapper.toDto(validDebt)).thenReturn(validDto);

            DebtDTO result = debtService.updateDebtFromDto(validDto);

            assertNotNull(result);
            verify(debtRepository).findById(1L);
            verify(debtMapper).updateEntityFromDto(validDebt, validDto);
            verify(debtRepository).save(validDebt);
            verify(debtMapper).toDto(validDebt);
        }

        @Test
        @DisplayName("Should throw exception when DTO is null")
        void shouldThrowExceptionWhenDtoIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.updateDebtFromDto(null)
            );
            assertEquals("DebtDTO must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when DTO has no debt ID")
        void shouldThrowExceptionWhenDtoHasNoId() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.updateDebtFromDto(validDto)
            );
            assertEquals("Debt ID must not be null for update operation", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when debt does not exist")
        void shouldThrowExceptionWhenDebtNotFound() {
            validDto.setDebtId(999L);
            when(debtRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.updateDebtFromDto(validDto)
            );
            assertEquals("Debt with ID 999 does not exist", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("findByIdAsDto Tests")
    class FindByIdAsDtoTests {

        @Test
        @DisplayName("Should successfully find debt by ID")
        void shouldFindDebtById() {
            when(debtRepository.findById(1L)).thenReturn(Optional.of(validDebt));
            when(debtMapper.toDto(validDebt)).thenReturn(validDto);

            Optional<DebtDTO> result = debtService.findByIdAsDto(1L);

            assertTrue(result.isPresent());
            assertEquals(validDto, result.get());
            verify(debtRepository).findById(1L);
            verify(debtMapper).toDto(validDebt);
        }

        @Test
        @DisplayName("Should return empty optional when debt not found")
        void shouldReturnEmptyWhenNotFound() {
            when(debtRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<DebtDTO> result = debtService.findByIdAsDto(999L);

            assertFalse(result.isPresent());
            verify(debtRepository).findById(999L);
            verify(debtMapper, never()).toDto(any());
        }

        @Test
        @DisplayName("Should throw exception when ID is null")
        void shouldThrowExceptionWhenIdIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.findByIdAsDto(null)
            );
            assertEquals("Debt ID must not be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("findAllAsDto Tests")
    class FindAllAsDtoTests {

        @Test
        @DisplayName("Should successfully find all debts")
        void shouldFindAllDebts() {
            Debt debt2 = new Debt();
            debt2.setDebtId(2L);
            List<Debt> debts = Arrays.asList(validDebt, debt2);

            DebtDTO dto2 = DebtDTO.builder()
                    .debtId(2L)
                    .debtName("Student Loan")
                    .build();

            when(debtRepository.findAllByDeletedFalse()).thenReturn(debts);
            when(debtMapper.toDto(validDebt)).thenReturn(validDto);
            when(debtMapper.toDto(debt2)).thenReturn(dto2);

            List<DebtDTO> result = debtService.findAllAsDto();

            assertEquals(2, result.size());
            verify(debtRepository).findAllByDeletedFalse();
            verify(debtMapper, times(2)).toDto(any(Debt.class));
        }

        @Test
        @DisplayName("Should return empty list when no debts exist")
        void shouldReturnEmptyListWhenNoDebts() {
            when(debtRepository.findAllByDeletedFalse()).thenReturn(Collections.emptyList());

            List<DebtDTO> result = debtService.findAllAsDto();

            assertTrue(result.isEmpty());
            verify(debtRepository).findAllByDeletedFalse();
        }
    }

    @Nested
    @DisplayName("findAllByUserAsDto Tests")
    class FindAllByUserAsDtoTests {

        @Test
        @DisplayName("Should return debts for the given user")
        void shouldReturnDebtsForUser() {
            Debt debt2 = new Debt();
            debt2.setDebtId(2L);
            debt2.setUser(testUser);
            List<Debt> debts = Arrays.asList(validDebt, debt2);

            DebtDTO dto2 = DebtDTO.builder()
                    .debtId(2L)
                    .debtName("Student Loan")
                    .build();

            when(debtRepository.findAllByUserAndDeletedFalse(testUser)).thenReturn(debts);
            when(debtMapper.toDto(validDebt)).thenReturn(validDto);
            when(debtMapper.toDto(debt2)).thenReturn(dto2);

            List<DebtDTO> result = debtService.findAllByUserAsDto(testUser);

            assertEquals(2, result.size());
            verify(debtRepository).findAllByUserAndDeletedFalse(testUser);
            verify(debtMapper, times(2)).toDto(any(Debt.class));
        }

        @Test
        @DisplayName("Should return empty list when user has no debts")
        void shouldReturnEmptyListWhenUserHasNoDebts() {
            when(debtRepository.findAllByUserAndDeletedFalse(testUser)).thenReturn(Collections.emptyList());

            List<DebtDTO> result = debtService.findAllByUserAsDto(testUser);

            assertTrue(result.isEmpty());
            verify(debtRepository).findAllByUserAndDeletedFalse(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.findAllByUserAsDto(null)
            );
            assertEquals("User must not be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("deleteById Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should successfully soft delete debt by ID")
        void shouldDeleteDebtById() {
            when(debtRepository.findById(1L)).thenReturn(Optional.of(validDebt));
            when(debtRepository.save(validDebt)).thenReturn(validDebt);

            assertDoesNotThrow(() -> debtService.deleteById(1L));

            verify(debtRepository).findById(1L);
            verify(debtRepository).save(validDebt);
            assertTrue(validDebt.getDeleted());
        }

        @Test
        @DisplayName("Should throw exception when ID is null")
        void shouldThrowExceptionWhenIdIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.deleteById(null)
            );
            assertEquals("Debt ID must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when debt does not exist")
        void shouldThrowExceptionWhenDebtNotFound() {
            when(debtRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.deleteById(999L)
            );
            assertEquals("Debt with ID 999 does not exist", exception.getMessage());
            verify(debtRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Interest Calculation Tests")
    class InterestCalculationTests {

        @Test
        @DisplayName("Should calculate monthly interest correctly")
        void shouldCalculateMonthlyInterestCorrectly() {
            BigDecimal balance = new BigDecimal("10000.00");
            BigDecimal annualRate = new BigDecimal("12.00");

            BigDecimal monthlyInterest = debtService.calculateMonthlyInterest(balance, annualRate);

            assertEquals(new BigDecimal("100.00"), monthlyInterest);
        }

        @Test
        @DisplayName("Should calculate daily interest correctly")
        void shouldCalculateDailyInterestCorrectly() {
            BigDecimal balance = new BigDecimal("10000.00");
            BigDecimal annualRate = new BigDecimal("18.25");

            BigDecimal dailyInterest = debtService.calculateDailyInterest(balance, annualRate);

            assertEquals(new BigDecimal("5.00"), dailyInterest);
        }

        @Test
        @DisplayName("Should calculate accrued interest over time period")
        void shouldCalculateAccruedInterest() {
            BigDecimal balance = new BigDecimal("10000.00");
            BigDecimal annualRate = new BigDecimal("18.25");
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31);

            BigDecimal accruedInterest = debtService.calculateAccruedInterest(
                    balance, annualRate, startDate, endDate
            );

            assertEquals(new BigDecimal("150.00"), accruedInterest);
        }

        @Test
        @DisplayName("Should return zero interest for zero balance")
        void shouldReturnZeroInterestForZeroBalance() {
            BigDecimal monthlyInterest = debtService.calculateMonthlyInterest(
                    BigDecimal.ZERO, new BigDecimal("18.99")
            );

            assertEquals(BigDecimal.ZERO.setScale(2), monthlyInterest);
        }

        @Test
        @DisplayName("Should throw exception when balance is negative")
        void shouldThrowExceptionWhenBalanceIsNegative() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.calculateMonthlyInterest(
                            new BigDecimal("-1000.00"), new BigDecimal("18.99")
                    )
            );
            assertEquals("Balance must not be negative", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when interest rate is negative")
        void shouldThrowExceptionWhenInterestRateIsNegative() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.calculateMonthlyInterest(
                            new BigDecimal("1000.00"), new BigDecimal("-5.00")
                    )
            );
            assertEquals("Interest rate must not be negative", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when parameters are null")
        void shouldThrowExceptionWhenParametersAreNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.calculateMonthlyInterest(null, new BigDecimal("18.99"))
            );
            assertEquals("Balance and rate must not be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getTotalDebtByUser Tests")
    class GetTotalDebtByUserTests {

        @Test
        @DisplayName("Should get total debt for user")
        void shouldGetTotalDebtForUser() {
            BigDecimal expectedTotal = new BigDecimal("25000.00");
            when(debtRepository.getTotalDebtByUser(testUser)).thenReturn(expectedTotal);

            BigDecimal result = debtService.getTotalDebtByUser(testUser);

            assertEquals(expectedTotal, result);
            verify(debtRepository).getTotalDebtByUser(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtService.getTotalDebtByUser(null)
            );
            assertEquals("User must not be null", exception.getMessage());
        }
    }
}
