package com.jameselner.finance_hub.integration.workflow;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.DebtType;
import com.jameselner.finance_hub.dto.DebtDTO;
import com.jameselner.finance_hub.dto.UserRegistrationDto;
import com.jameselner.finance_hub.integration.TransactionalIntegrationTest;
import com.jameselner.finance_hub.repository.DebtRepository;
import com.jameselner.finance_hub.service.DebtService;
import com.jameselner.finance_hub.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Debt Management Workflow Integration Tests")
class DebtManagementWorkflowTest extends TransactionalIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private DebtService debtService;

    @Autowired
    private DebtRepository debtRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("debt-workflow@test.com");
        dto.setPassword("SecurePass@123");
        dto.setFirstName("Debt");
        dto.setLastName("Manager");
        testUser = userService.registerUser(dto);
    }

    @Test
    @DisplayName("Should manage complete debt lifecycle from creation to tracking")
    void shouldManageCompleteDebtLifecycle() {
        // Step 1: Create initial debt
        DebtDTO creditCard = DebtDTO.builder()
                .userId(testUser.getUserId())
                .debtName("Visa Credit Card")
                .debtType(DebtType.CREDIT_CARD)
                .principalAmount(new BigDecimal("5000.00"))
                .currentBalance(new BigDecimal("5000.00"))
                .interestRate(new BigDecimal("19.99"))
                .startDate(LocalDate.now())
                .minimumPayment(new BigDecimal("100.00"))
                .build();
        DebtDTO createdDebt = debtService.createDebtFromDto(creditCard);
        assertNotNull(createdDebt.getDebtId());

        // Step 2: Simulate payments by updating balance
        createdDebt.setCurrentBalance(new BigDecimal("4500.00"));
        DebtDTO afterPayment1 = debtService.updateDebtFromDto(createdDebt);
        assertEquals(new BigDecimal("4500.00"), afterPayment1.getCurrentBalance());

        // Step 3: More payments
        afterPayment1.setCurrentBalance(new BigDecimal("4000.00"));
        DebtDTO afterPayment2 = debtService.updateDebtFromDto(afterPayment1);
        assertEquals(new BigDecimal("4000.00"), afterPayment2.getCurrentBalance());

        // Step 4: Verify total debt calculation
        BigDecimal totalDebt = debtService.getTotalDebtByUser(testUser);
        assertEquals(new BigDecimal("4000.00"), totalDebt);
    }

    @Test
    @DisplayName("Should manage multiple debts with different types")
    void shouldManageMultipleDebtsWithDifferentTypes() {
        // Create credit card debt
        DebtDTO creditCard = DebtDTO.builder()
                .userId(testUser.getUserId())
                .debtName("Credit Card")
                .debtType(DebtType.CREDIT_CARD)
                .principalAmount(new BigDecimal("3000.00"))
                .currentBalance(new BigDecimal("2500.00"))
                .interestRate(new BigDecimal("22.99"))
                .startDate(LocalDate.now().minusMonths(6))
                .minimumPayment(new BigDecimal("75.00"))
                .build();
        debtService.createDebtFromDto(creditCard);

        // Create personal loan
        DebtDTO personalLoan = DebtDTO.builder()
                .userId(testUser.getUserId())
                .debtName("Personal Loan")
                .debtType(DebtType.PERSONAL_LOAN)
                .principalAmount(new BigDecimal("10000.00"))
                .currentBalance(new BigDecimal("8000.00"))
                .interestRate(new BigDecimal("9.99"))
                .startDate(LocalDate.now().minusYears(1))
                .minimumPayment(new BigDecimal("250.00"))
                .build();
        debtService.createDebtFromDto(personalLoan);

        // Create auto loan
        DebtDTO autoLoan = DebtDTO.builder()
                .userId(testUser.getUserId())
                .debtName("Car Loan")
                .debtType(DebtType.AUTO_LOAN)
                .principalAmount(new BigDecimal("20000.00"))
                .currentBalance(new BigDecimal("15000.00"))
                .interestRate(new BigDecimal("6.49"))
                .startDate(LocalDate.now().minusYears(2))
                .minimumPayment(new BigDecimal("400.00"))
                .build();
        debtService.createDebtFromDto(autoLoan);

        // Verify all debts
        List<DebtDTO> allDebts = debtService.findAllAsDto();
        assertEquals(3, allDebts.size());

        // Verify total debt
        BigDecimal totalDebt = debtService.getTotalDebtByUser(testUser);
        assertEquals(new BigDecimal("25500.00"), totalDebt);
    }

    @Test
    @DisplayName("Should calculate interest for different debt types")
    void shouldCalculateInterestForDifferentDebtTypes() {
        // High interest credit card
        BigDecimal ccBalance = new BigDecimal("5000.00");
        BigDecimal ccRate = new BigDecimal("24.00");
        BigDecimal ccMonthlyInterest = debtService.calculateMonthlyInterest(ccBalance, ccRate);
        // 5000 * (24/100/12) = 100
        assertEquals(0, new BigDecimal("100.00").compareTo(ccMonthlyInterest));

        // Low interest mortgage
        BigDecimal mortgageBalance = new BigDecimal("200000.00");
        BigDecimal mortgageRate = new BigDecimal("3.00");
        BigDecimal mortgageMonthlyInterest = debtService.calculateMonthlyInterest(mortgageBalance, mortgageRate);
        // 200000 * (3/100/12) = 500
        assertEquals(0, new BigDecimal("500.00").compareTo(mortgageMonthlyInterest));

        // Compare - mortgage has higher absolute interest despite lower rate
        assertTrue(mortgageMonthlyInterest.compareTo(ccMonthlyInterest) > 0);
    }

    @Test
    @DisplayName("Should track debt payoff progress")
    void shouldTrackDebtPayoffProgress() {
        // Create debt
        DebtDTO debt = DebtDTO.builder()
                .userId(testUser.getUserId())
                .debtName("Test Loan")
                .debtType(DebtType.PERSONAL_LOAN)
                .principalAmount(new BigDecimal("10000.00"))
                .currentBalance(new BigDecimal("10000.00"))
                .interestRate(new BigDecimal("10.00"))
                .startDate(LocalDate.now())
                .minimumPayment(new BigDecimal("200.00"))
                .targetPayoffDate(LocalDate.now().plusYears(5))
                .build();
        DebtDTO created = debtService.createDebtFromDto(debt);

        // Simulate 5 monthly payments of $200 principal
        BigDecimal currentBalance = new BigDecimal("10000.00");
        for (int i = 0; i < 5; i++) {
            currentBalance = currentBalance.subtract(new BigDecimal("200.00"));
            created.setCurrentBalance(currentBalance);
            created = debtService.updateDebtFromDto(created);
        }

        // Verify progress
        DebtDTO finalDebt = debtService.findByIdAsDto(created.getDebtId()).orElseThrow();
        assertEquals(new BigDecimal("9000.00"), finalDebt.getCurrentBalance());

        // Paid off 10% of principal
        BigDecimal paidOff = finalDebt.getPrincipalAmount().subtract(finalDebt.getCurrentBalance());
        assertEquals(new BigDecimal("1000.00"), paidOff);
    }

    @Test
    @DisplayName("Should handle debt deletion")
    void shouldHandleDebtDeletion() {
        // Create two debts
        DebtDTO debt1 = DebtDTO.builder()
                .userId(testUser.getUserId())
                .debtName("Debt 1")
                .debtType(DebtType.OTHER)
                .principalAmount(new BigDecimal("1000.00"))
                .currentBalance(new BigDecimal("1000.00"))
                .interestRate(new BigDecimal("5.00"))
                .startDate(LocalDate.now())
                .minimumPayment(new BigDecimal("50.00"))
                .build();
        DebtDTO created1 = debtService.createDebtFromDto(debt1);

        DebtDTO debt2 = DebtDTO.builder()
                .userId(testUser.getUserId())
                .debtName("Debt 2")
                .debtType(DebtType.OTHER)
                .principalAmount(new BigDecimal("2000.00"))
                .currentBalance(new BigDecimal("2000.00"))
                .interestRate(new BigDecimal("5.00"))
                .startDate(LocalDate.now())
                .minimumPayment(new BigDecimal("100.00"))
                .build();
        debtService.createDebtFromDto(debt2);

        // Verify both exist
        assertEquals(2, debtService.findAllAsDto().size());
        assertEquals(new BigDecimal("3000.00"), debtService.getTotalDebtByUser(testUser));

        // Delete first debt (paid off or refinanced)
        debtService.deleteById(created1.getDebtId());

        // Verify only one remains
        assertEquals(1, debtService.findAllAsDto().size());
        assertEquals(new BigDecimal("2000.00"), debtService.getTotalDebtByUser(testUser));
    }

    @Test
    @DisplayName("Should handle student loan with specific characteristics")
    void shouldHandleStudentLoanWithSpecificCharacteristics() {
        DebtDTO studentLoan = DebtDTO.builder()
                .userId(testUser.getUserId())
                .debtName("Federal Student Loan")
                .debtType(DebtType.STUDENT_LOAN)
                .principalAmount(new BigDecimal("35000.00"))
                .currentBalance(new BigDecimal("32000.00"))
                .interestRate(new BigDecimal("4.99"))
                .startDate(LocalDate.now().minusYears(3))
                .targetPayoffDate(LocalDate.now().plusYears(7))
                .minimumPayment(new BigDecimal("350.00"))
                .build();
        DebtDTO created = debtService.createDebtFromDto(studentLoan);

        assertEquals(DebtType.STUDENT_LOAN, created.getDebtType());
        assertEquals(new BigDecimal("4.99"), created.getInterestRate());

        // Calculate monthly interest
        BigDecimal monthlyInterest = debtService.calculateMonthlyInterest(
                created.getCurrentBalance(), created.getInterestRate());
        // 32000 * (4.99/100/12) = ~133.07
        assertTrue(monthlyInterest.compareTo(new BigDecimal("130.00")) > 0);
        assertTrue(monthlyInterest.compareTo(new BigDecimal("140.00")) < 0);
    }
}
