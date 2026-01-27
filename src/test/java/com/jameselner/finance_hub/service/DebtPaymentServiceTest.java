package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.domain.DebtPayment;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.DebtType;
import com.jameselner.finance_hub.dto.DebtPaymentDTO;
import com.jameselner.finance_hub.mapper.DebtPaymentMapper;
import com.jameselner.finance_hub.repository.DebtPaymentRepository;
import com.jameselner.finance_hub.repository.DebtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DebtPaymentService Tests")
class DebtPaymentServiceTest {

    @Mock
    private DebtPaymentRepository debtPaymentRepository;

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private DebtPaymentMapper debtPaymentMapper;

    @Mock
    private DebtService debtService;

    @InjectMocks
    private DebtPaymentService debtPaymentService;

    private Debt testDebt;
    private DebtPayment testPayment;
    private DebtPaymentDTO testPaymentDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);

        testDebt = new Debt();
        testDebt.setDebtId(1L);
        testDebt.setUser(testUser);
        testDebt.setDebtName("Test Credit Card");
        testDebt.setDebtType(DebtType.CREDIT_CARD);
        testDebt.setPrincipalAmount(new BigDecimal("5000.00"));
        testDebt.setCurrentBalance(new BigDecimal("3000.00"));
        testDebt.setInterestRate(new BigDecimal("18.00"));
        testDebt.setStartDate(LocalDate.now().minusMonths(6));
        testDebt.setDeleted(false);

        testPayment = new DebtPayment();
        testPayment.setPaymentId(1L);
        testPayment.setDebt(testDebt);
        testPayment.setPaymentAmount(new BigDecimal("200.00"));
        testPayment.setPrincipalPaid(new BigDecimal("150.00"));
        testPayment.setInterestPaid(new BigDecimal("50.00"));
        testPayment.setPaymentDate(LocalDate.now());
        testPayment.setDeleted(false);

        testPaymentDto = DebtPaymentDTO.builder()
                .paymentId(1L)
                .debtId(1L)
                .debtName("Test Credit Card")
                .paymentAmount(new BigDecimal("200.00"))
                .principalPaid(new BigDecimal("150.00"))
                .interestPaid(new BigDecimal("50.00"))
                .paymentDate(LocalDate.now())
                .build();
    }

    @Nested
    @DisplayName("recordPayment Tests")
    class RecordPaymentTests {

        @Test
        @DisplayName("Should record payment with manual interest correctly")
        void shouldRecordPaymentWithManualInterest() {
            BigDecimal paymentAmount = new BigDecimal("200.00");
            BigDecimal interestPaid = new BigDecimal("50.00");
            LocalDate paymentDate = LocalDate.now();

            when(debtRepository.findById(1L)).thenReturn(Optional.of(testDebt));
            when(debtPaymentRepository.save(any(DebtPayment.class))).thenReturn(testPayment);
            when(debtPaymentMapper.toDto(testPayment)).thenReturn(testPaymentDto);

            DebtPaymentDTO result = debtPaymentService.recordPayment(1L, paymentAmount, interestPaid, paymentDate);

            assertNotNull(result);
            assertEquals(testPaymentDto, result);

            ArgumentCaptor<DebtPayment> paymentCaptor = ArgumentCaptor.forClass(DebtPayment.class);
            verify(debtPaymentRepository).save(paymentCaptor.capture());

            DebtPayment capturedPayment = paymentCaptor.getValue();
            assertEquals(paymentAmount, capturedPayment.getPaymentAmount());
            assertEquals(interestPaid, capturedPayment.getInterestPaid());
            assertEquals(new BigDecimal("150.00"), capturedPayment.getPrincipalPaid());
        }

        @Test
        @DisplayName("Should update debt balance after payment")
        void shouldUpdateDebtBalanceAfterPayment() {
            BigDecimal paymentAmount = new BigDecimal("200.00");
            BigDecimal interestPaid = new BigDecimal("50.00");
            BigDecimal expectedPrincipal = new BigDecimal("150.00");
            BigDecimal originalBalance = testDebt.getCurrentBalance();
            BigDecimal expectedNewBalance = originalBalance.subtract(expectedPrincipal);

            when(debtRepository.findById(1L)).thenReturn(Optional.of(testDebt));
            when(debtPaymentRepository.save(any(DebtPayment.class))).thenReturn(testPayment);
            when(debtPaymentMapper.toDto(testPayment)).thenReturn(testPaymentDto);

            debtPaymentService.recordPayment(1L, paymentAmount, interestPaid, LocalDate.now());

            verify(debtRepository).save(testDebt);
            assertEquals(expectedNewBalance, testDebt.getCurrentBalance());
        }

        @Test
        @DisplayName("Should throw exception when debt ID is null")
        void shouldThrowExceptionWhenDebtIdIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.recordPayment(null, new BigDecimal("100"), BigDecimal.ZERO, LocalDate.now())
            );
            assertEquals("Debt ID must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when payment amount is null")
        void shouldThrowExceptionWhenPaymentAmountIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.recordPayment(1L, null, BigDecimal.ZERO, LocalDate.now())
            );
            assertEquals("Payment amount must be greater than zero", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when payment amount is zero")
        void shouldThrowExceptionWhenPaymentAmountIsZero() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.recordPayment(1L, BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now())
            );
            assertEquals("Payment amount must be greater than zero", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when interest paid is null")
        void shouldThrowExceptionWhenInterestPaidIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.recordPayment(1L, new BigDecimal("100"), null, LocalDate.now())
            );
            assertEquals("Interest paid must not be negative", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when interest paid is negative")
        void shouldThrowExceptionWhenInterestPaidIsNegative() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.recordPayment(1L, new BigDecimal("100"), new BigDecimal("-10"), LocalDate.now())
            );
            assertEquals("Interest paid must not be negative", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when interest exceeds payment amount")
        void shouldThrowExceptionWhenInterestExceedsPayment() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.recordPayment(1L, new BigDecimal("100"), new BigDecimal("150"), LocalDate.now())
            );
            assertEquals("Interest paid cannot exceed payment amount", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when payment date is null")
        void shouldThrowExceptionWhenPaymentDateIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.recordPayment(1L, new BigDecimal("100"), BigDecimal.ZERO, null)
            );
            assertEquals("Payment date must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when debt does not exist")
        void shouldThrowExceptionWhenDebtNotFound() {
            when(debtRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.recordPayment(999L, new BigDecimal("100"), BigDecimal.ZERO, LocalDate.now())
            );
            assertEquals("Debt with ID 999 does not exist", exception.getMessage());
        }

        @Test
        @DisplayName("Should allow full payment to go to principal when interest is zero")
        void shouldAllowFullPaymentToPrincipal() {
            BigDecimal paymentAmount = new BigDecimal("200.00");
            BigDecimal interestPaid = BigDecimal.ZERO;

            when(debtRepository.findById(1L)).thenReturn(Optional.of(testDebt));
            when(debtPaymentRepository.save(any(DebtPayment.class))).thenReturn(testPayment);
            when(debtPaymentMapper.toDto(testPayment)).thenReturn(testPaymentDto);

            debtPaymentService.recordPayment(1L, paymentAmount, interestPaid, LocalDate.now());

            ArgumentCaptor<DebtPayment> paymentCaptor = ArgumentCaptor.forClass(DebtPayment.class);
            verify(debtPaymentRepository).save(paymentCaptor.capture());

            DebtPayment capturedPayment = paymentCaptor.getValue();
            assertEquals(paymentAmount, capturedPayment.getPrincipalPaid());
            assertEquals(BigDecimal.ZERO, capturedPayment.getInterestPaid());
        }

        @Test
        @DisplayName("Should allow full payment to go to interest")
        void shouldAllowFullPaymentToInterest() {
            BigDecimal paymentAmount = new BigDecimal("200.00");
            BigDecimal interestPaid = new BigDecimal("200.00");

            when(debtRepository.findById(1L)).thenReturn(Optional.of(testDebt));
            when(debtPaymentRepository.save(any(DebtPayment.class))).thenReturn(testPayment);
            when(debtPaymentMapper.toDto(testPayment)).thenReturn(testPaymentDto);

            debtPaymentService.recordPayment(1L, paymentAmount, interestPaid, LocalDate.now());

            ArgumentCaptor<DebtPayment> paymentCaptor = ArgumentCaptor.forClass(DebtPayment.class);
            verify(debtPaymentRepository).save(paymentCaptor.capture());

            DebtPayment capturedPayment = paymentCaptor.getValue();
            assertEquals(0, BigDecimal.ZERO.compareTo(capturedPayment.getPrincipalPaid()));
            assertEquals(0, paymentAmount.compareTo(capturedPayment.getInterestPaid()));
        }
    }

    @Nested
    @DisplayName("updatePayment Tests")
    class UpdatePaymentTests {

        @Test
        @DisplayName("Should update payment with new values")
        void shouldUpdatePaymentWithNewValues() {
            BigDecimal newAmount = new BigDecimal("300.00");
            BigDecimal newInterest = new BigDecimal("75.00");
            LocalDate newDate = LocalDate.now().minusDays(1);

            when(debtPaymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
            when(debtPaymentRepository.save(any(DebtPayment.class))).thenReturn(testPayment);
            when(debtPaymentMapper.toDto(testPayment)).thenReturn(testPaymentDto);

            DebtPaymentDTO result = debtPaymentService.updatePayment(1L, newAmount, newInterest, newDate);

            assertNotNull(result);
            verify(debtPaymentRepository).save(testPayment);
            verify(debtRepository).save(testDebt);
        }

        @Test
        @DisplayName("Should restore old principal before applying new principal")
        void shouldRestoreOldPrincipalBeforeApplyingNew() {
            BigDecimal oldPrincipal = testPayment.getPrincipalPaid();
            BigDecimal originalBalance = testDebt.getCurrentBalance();
            BigDecimal newAmount = new BigDecimal("300.00");
            BigDecimal newInterest = new BigDecimal("100.00");
            BigDecimal newPrincipal = newAmount.subtract(newInterest);

            BigDecimal expectedFinalBalance = originalBalance.add(oldPrincipal).subtract(newPrincipal);

            when(debtPaymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
            when(debtPaymentRepository.save(any(DebtPayment.class))).thenReturn(testPayment);
            when(debtPaymentMapper.toDto(testPayment)).thenReturn(testPaymentDto);

            debtPaymentService.updatePayment(1L, newAmount, newInterest, LocalDate.now());

            assertEquals(expectedFinalBalance, testDebt.getCurrentBalance());
        }

        @Test
        @DisplayName("Should throw exception when payment ID is null")
        void shouldThrowExceptionWhenPaymentIdIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.updatePayment(null, new BigDecimal("100"), BigDecimal.ZERO, LocalDate.now())
            );
            assertEquals("Payment ID must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when interest exceeds amount")
        void shouldThrowExceptionWhenInterestExceedsAmount() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.updatePayment(1L, new BigDecimal("100"), new BigDecimal("150"), LocalDate.now())
            );
            assertEquals("Interest paid cannot exceed payment amount", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            when(debtPaymentRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.updatePayment(999L, new BigDecimal("100"), BigDecimal.ZERO, LocalDate.now())
            );
            assertEquals("Payment with ID 999 does not exist", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when trying to update deleted payment")
        void shouldThrowExceptionWhenPaymentIsDeleted() {
            testPayment.setDeleted(true);
            when(debtPaymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.updatePayment(1L, new BigDecimal("100"), BigDecimal.ZERO, LocalDate.now())
            );
            assertEquals("Cannot update deleted payment", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("findByDebtId Tests")
    class FindByDebtIdTests {

        @Test
        @DisplayName("Should find all payments for a debt")
        void shouldFindAllPaymentsForDebt() {
            DebtPayment payment2 = new DebtPayment();
            payment2.setPaymentId(2L);
            payment2.setDebt(testDebt);
            payment2.setDeleted(false);

            DebtPaymentDTO dto2 = DebtPaymentDTO.builder().paymentId(2L).build();

            when(debtPaymentRepository.findByDebtDebtIdAndDeletedFalseOrderByPaymentDateDesc(1L))
                    .thenReturn(Arrays.asList(testPayment, payment2));
            when(debtPaymentMapper.toDto(testPayment)).thenReturn(testPaymentDto);
            when(debtPaymentMapper.toDto(payment2)).thenReturn(dto2);

            List<DebtPaymentDTO> result = debtPaymentService.findByDebtId(1L);

            assertEquals(2, result.size());
            verify(debtPaymentRepository).findByDebtDebtIdAndDeletedFalseOrderByPaymentDateDesc(1L);
        }

        @Test
        @DisplayName("Should throw exception when debt ID is null")
        void shouldThrowExceptionWhenDebtIdIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.findByDebtId(null)
            );
            assertEquals("Debt ID must not be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("deleteById Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should soft delete payment and restore debt balance")
        void shouldSoftDeleteAndRestoreBalance() {
            BigDecimal principalToRestore = testPayment.getPrincipalPaid();
            BigDecimal originalBalance = testDebt.getCurrentBalance();
            BigDecimal expectedBalance = originalBalance.add(principalToRestore);

            when(debtPaymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

            debtPaymentService.deleteById(1L);

            assertTrue(testPayment.getDeleted());
            assertEquals(expectedBalance, testDebt.getCurrentBalance());
            verify(debtPaymentRepository).save(testPayment);
            verify(debtRepository).save(testDebt);
        }

        @Test
        @DisplayName("Should throw exception when payment ID is null")
        void shouldThrowExceptionWhenIdIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.deleteById(null)
            );
            assertEquals("Debt payment ID must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            when(debtPaymentRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtPaymentService.deleteById(999L)
            );
            assertEquals("Debt payment with ID 999 does not exist", exception.getMessage());
        }
    }
}
