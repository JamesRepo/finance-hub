package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Account;
import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.Transaction;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.TransactionType;
import com.jameselner.finance_hub.dto.TransactionDTO;
import com.jameselner.finance_hub.mapper.TransactionMapper;
import com.jameselner.finance_hub.repository.TransactionRepository;
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
@DisplayName("TransactionService Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private TransactionDTO validDto;
    private Transaction validTransaction;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);

        validDto = TransactionDTO.builder()
                .accountId(1L)
                .accountName("Test Account")
                .categoryId(1L)
                .categoryName("Test Category")
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .description("Test transaction")
                .build();

        validTransaction = new Transaction();
        validTransaction.setTransactionId(1L);
        validTransaction.setAmount(new BigDecimal("100.00"));
        validTransaction.setTransactionDate(LocalDate.now());
        validTransaction.setTransactionType(TransactionType.EXPENSE);

        Account account = new Account();
        account.setAccountId(1L);
        account.setAccountName("Test Account");

        Category category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Test Category");

        validTransaction.setAccount(account);
        validTransaction.setCategory(category);
    }

    @Nested
    @DisplayName("createTransactionFromDto Tests")
    class CreateTransactionFromDtoTests {

        @Test
        @DisplayName("Should successfully create transaction from valid DTO")
        void shouldCreateTransactionSuccessfully() {
            when(transactionMapper.toEntity(validDto)).thenReturn(validTransaction);
            when(transactionRepository.save(validTransaction)).thenReturn(validTransaction);
            when(transactionMapper.toDto(validTransaction)).thenReturn(validDto);

            TransactionDTO result = transactionService.createTransactionFromDto(validDto);

            assertNotNull(result);
            verify(transactionMapper).toEntity(validDto);
            verify(transactionRepository).save(validTransaction);
            verify(transactionMapper).toDto(validTransaction);
        }

        @Test
        @DisplayName("Should throw exception when DTO is null")
        void shouldThrowExceptionWhenDtoIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.createTransactionFromDto(null)
            );
            assertEquals("TransactionDTO must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when DTO has transaction ID set")
        void shouldThrowExceptionWhenDtoHasId() {
            validDto.setTransactionId(1L);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.createTransactionFromDto(validDto)
            );
            assertEquals("Transaction ID must be null for create operation", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when account ID is null")
        void shouldThrowExceptionWhenAccountIdIsNull() {
            validDto.setAccountId(null);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.createTransactionFromDto(validDto)
            );
            assertEquals("Account ID must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when category ID is null")
        void shouldThrowExceptionWhenCategoryIdIsNull() {
            validDto.setCategoryId(null);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.createTransactionFromDto(validDto)
            );
            assertEquals("Category ID must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when transaction type is null")
        void shouldThrowExceptionWhenTransactionTypeIsNull() {
            validDto.setTransactionType(null);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.createTransactionFromDto(validDto)
            );
            assertEquals("Transaction type must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            validDto.setAmount(null);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.createTransactionFromDto(validDto)
            );
            assertEquals("Amount must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when transaction date is null")
        void shouldThrowExceptionWhenTransactionDateIsNull() {
            validDto.setTransactionDate(null);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.createTransactionFromDto(validDto)
            );
            assertEquals("Transaction date must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when amount is zero")
        void shouldThrowExceptionWhenAmountIsZero() {
            validDto.setAmount(BigDecimal.ZERO);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.createTransactionFromDto(validDto)
            );
            assertEquals("Amount must be greater than zero", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when amount is negative")
        void shouldThrowExceptionWhenAmountIsNegative() {
            validDto.setAmount(new BigDecimal("-50.00"));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.createTransactionFromDto(validDto)
            );
            assertEquals("Amount must be greater than zero", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("updateTransactionFromDto Tests")
    class UpdateTransactionFromDtoTests {

        @Test
        @DisplayName("Should successfully update transaction from valid DTO")
        void shouldUpdateTransactionSuccessfully() {
            validDto.setTransactionId(1L);

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(validTransaction));
            doNothing().when(transactionMapper).updateEntityFromDto(validTransaction, validDto);
            when(transactionRepository.save(validTransaction)).thenReturn(validTransaction);
            when(transactionMapper.toDto(validTransaction)).thenReturn(validDto);

            TransactionDTO result = transactionService.updateTransactionFromDto(validDto);

            assertNotNull(result);
            verify(transactionRepository).findById(1L);
            verify(transactionMapper).updateEntityFromDto(validTransaction, validDto);
            verify(transactionRepository).save(validTransaction);
            verify(transactionMapper).toDto(validTransaction);
        }

        @Test
        @DisplayName("Should throw exception when DTO is null")
        void shouldThrowExceptionWhenDtoIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.updateTransactionFromDto(null)
            );
            assertEquals("TransactionDTO must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when DTO has no transaction ID")
        void shouldThrowExceptionWhenDtoHasNoId() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.updateTransactionFromDto(validDto)
            );
            assertEquals("Transaction ID must not be null for update operation", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when transaction does not exist")
        void shouldThrowExceptionWhenTransactionNotFound() {
            validDto.setTransactionId(999L);
            when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.updateTransactionFromDto(validDto)
            );
            assertEquals("Transaction with ID 999 does not exist", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when required fields are missing")
        void shouldThrowExceptionWhenRequiredFieldsMissing() {
            validDto.setTransactionId(1L);
            validDto.setAccountId(null);

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(validTransaction));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.updateTransactionFromDto(validDto)
            );
            assertEquals("Account ID must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when amount is invalid")
        void shouldThrowExceptionWhenAmountIsInvalid() {
            validDto.setTransactionId(1L);
            validDto.setAmount(BigDecimal.ZERO);

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(validTransaction));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.updateTransactionFromDto(validDto)
            );
            assertEquals("Amount must be greater than zero", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("findByIdAsDto Tests")
    class FindByIdAsDtoTests {

        @Test
        @DisplayName("Should successfully find transaction by ID")
        void shouldFindTransactionById() {
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(validTransaction));
            when(transactionMapper.toDto(validTransaction)).thenReturn(validDto);

            Optional<TransactionDTO> result = transactionService.findByIdAsDto(1L);

            assertTrue(result.isPresent());
            assertEquals(validDto, result.get());
            verify(transactionRepository).findById(1L);
            verify(transactionMapper).toDto(validTransaction);
        }

        @Test
        @DisplayName("Should return empty optional when transaction not found")
        void shouldReturnEmptyWhenNotFound() {
            when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<TransactionDTO> result = transactionService.findByIdAsDto(999L);

            assertFalse(result.isPresent());
            verify(transactionRepository).findById(999L);
            verify(transactionMapper, never()).toDto(any());
        }

        @Test
        @DisplayName("Should throw exception when ID is null")
        void shouldThrowExceptionWhenIdIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.findByIdAsDto(null)
            );
            assertEquals("Transaction ID must not be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("findAllAsDto Tests")
    class FindAllAsDtoTests {

        @Test
        @DisplayName("Should successfully find all transactions")
        void shouldFindAllTransactions() {
            Transaction transaction2 = new Transaction();
            transaction2.setTransactionId(2L);
            List<Transaction> transactions = Arrays.asList(validTransaction, transaction2);

            TransactionDTO dto2 = TransactionDTO.builder()
                    .transactionId(2L)
                    .amount(new BigDecimal("200.00"))
                    .build();

            when(transactionRepository.findAll()).thenReturn(transactions);
            when(transactionMapper.toDto(validTransaction)).thenReturn(validDto);
            when(transactionMapper.toDto(transaction2)).thenReturn(dto2);

            List<TransactionDTO> result = transactionService.findAllAsDto();

            assertEquals(2, result.size());
            verify(transactionRepository).findAll();
            verify(transactionMapper, times(2)).toDto(any(Transaction.class));
        }

        @Test
        @DisplayName("Should return empty list when no transactions exist")
        void shouldReturnEmptyListWhenNoTransactions() {
            when(transactionRepository.findAll()).thenReturn(Collections.emptyList());

            List<TransactionDTO> result = transactionService.findAllAsDto();

            assertTrue(result.isEmpty());
            verify(transactionRepository).findAll();
        }
    }

    @Nested
    @DisplayName("deleteById Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should successfully delete transaction by ID")
        void shouldDeleteTransactionById() {
            when(transactionRepository.existsById(1L)).thenReturn(true);
            doNothing().when(transactionRepository).deleteById(1L);

            assertDoesNotThrow(() -> transactionService.deleteById(1L));

            verify(transactionRepository).existsById(1L);
            verify(transactionRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when ID is null")
        void shouldThrowExceptionWhenIdIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.deleteById(null)
            );
            assertEquals("Transaction ID must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when transaction does not exist")
        void shouldThrowExceptionWhenTransactionNotFound() {
            when(transactionRepository.existsById(999L)).thenReturn(false);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.deleteById(999L)
            );
            assertEquals("Transaction with ID 999 does not exist", exception.getMessage());
            verify(transactionRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("sumByUserAndTypeAndDateRange Tests")
    class SumByUserAndTypeAndDateRangeTests {

        private LocalDate startDate;
        private LocalDate endDate;

        @BeforeEach
        void setUp() {
            startDate = LocalDate.of(2024, 1, 1);
            endDate = LocalDate.of(2024, 12, 31);
        }

        @Test
        @DisplayName("Should successfully calculate sum for valid parameters")
        void shouldCalculateSumSuccessfully() {
            BigDecimal expectedSum = new BigDecimal("1000.00");
            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    testUser, TransactionType.EXPENSE, startDate, endDate
            )).thenReturn(expectedSum);

            BigDecimal result = transactionService.sumByUserAndTypeAndDateRange(
                    testUser, TransactionType.EXPENSE, startDate, endDate
            );

            assertEquals(expectedSum, result);
            verify(transactionRepository).sumByUserAndTypeAndDateRange(
                    testUser, TransactionType.EXPENSE, startDate, endDate
            );
        }

        @Test
        @DisplayName("Should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.sumByUserAndTypeAndDateRange(
                            null, TransactionType.EXPENSE, startDate, endDate
                    )
            );
            assertEquals("User must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when transaction type is null")
        void shouldThrowExceptionWhenTypeIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.sumByUserAndTypeAndDateRange(
                            testUser, null, startDate, endDate
                    )
            );
            assertEquals("Transaction type must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when start date is null")
        void shouldThrowExceptionWhenStartDateIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.sumByUserAndTypeAndDateRange(
                            testUser, TransactionType.EXPENSE, null, endDate
                    )
            );
            assertEquals("Start date must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when end date is null")
        void shouldThrowExceptionWhenEndDateIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.sumByUserAndTypeAndDateRange(
                            testUser, TransactionType.EXPENSE, startDate, null
                    )
            );
            assertEquals("End date must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when start date is after end date")
        void shouldThrowExceptionWhenStartDateAfterEndDate() {
            LocalDate invalidStartDate = LocalDate.of(2024, 12, 31);
            LocalDate invalidEndDate = LocalDate.of(2024, 1, 1);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.sumByUserAndTypeAndDateRange(
                            testUser, TransactionType.EXPENSE, invalidStartDate, invalidEndDate
                    )
            );
            assertEquals("Start date must be before or equal to end date", exception.getMessage());
        }

        @Test
        @DisplayName("Should accept equal start and end dates")
        void shouldAcceptEqualDates() {
            LocalDate sameDate = LocalDate.of(2024, 6, 15);
            BigDecimal expectedSum = new BigDecimal("500.00");

            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    testUser, TransactionType.EXPENSE, sameDate, sameDate
            )).thenReturn(expectedSum);

            BigDecimal result = transactionService.sumByUserAndTypeAndDateRange(
                    testUser, TransactionType.EXPENSE, sameDate, sameDate
            );

            assertEquals(expectedSum, result);
        }
    }
}