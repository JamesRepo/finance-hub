package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.DebtType;
import com.jameselner.finance_hub.dto.DebtDTO;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DebtMapper Tests")
class DebtMapperTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DebtMapper debtMapper;

    private User testUser;
    private DebtDTO validDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");

        validDto = DebtDTO.builder()
                .userId(1L)
                .debtName("Credit Card")
                .debtType(DebtType.CREDIT_CARD)
                .currentBalance(new BigDecimal("5000.00"))
                .interestRate(new BigDecimal("18.99"))
                .principalAmount(new BigDecimal("10000.00"))
                .minimumPayment(new BigDecimal("100.00"))
                .startDate(LocalDate.of(2024, 1, 1))
                .notes("Test notes")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("updateEntityFromDto Tests")
    class UpdateEntityFromDtoTests {

        @Test
        @DisplayName("Should look up user when entity has no user set (create path)")
        void shouldLookUpUserWhenEntityHasNoUser() {
            Debt debt = new Debt();
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            debtMapper.updateEntityFromDto(debt, validDto);

            assertEquals(testUser, debt.getUser());
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should skip user lookup when entity already has a user (update path)")
        void shouldSkipUserLookupWhenEntityAlreadyHasUser() {
            Debt debt = new Debt();
            debt.setUser(testUser);

            debtMapper.updateEntityFromDto(debt, validDto);

            assertEquals(testUser, debt.getUser());
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should update all fields from DTO")
        void shouldUpdateAllFieldsFromDto() {
            Debt debt = new Debt();
            debt.setUser(testUser);

            debtMapper.updateEntityFromDto(debt, validDto);

            assertEquals("Credit Card", debt.getDebtName());
            assertEquals(DebtType.CREDIT_CARD, debt.getDebtType());
            assertEquals(new BigDecimal("5000.00"), debt.getCurrentBalance());
            assertEquals(new BigDecimal("18.99"), debt.getInterestRate());
            assertEquals(new BigDecimal("10000.00"), debt.getPrincipalAmount());
            assertEquals(new BigDecimal("100.00"), debt.getMinimumPayment());
            assertEquals(LocalDate.of(2024, 1, 1), debt.getStartDate());
            assertEquals("Test notes", debt.getNotes());
            assertTrue(debt.getActive());
        }

        @Test
        @DisplayName("Should not update active when DTO active is null")
        void shouldNotUpdateActiveWhenDtoActiveIsNull() {
            Debt debt = new Debt();
            debt.setUser(testUser);
            debt.setActive(true);

            validDto.setActive(null);

            debtMapper.updateEntityFromDto(debt, validDto);

            assertTrue(debt.getActive());
        }

        @Test
        @DisplayName("Should throw when debt is null")
        void shouldThrowWhenDebtIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtMapper.updateEntityFromDto(null, validDto)
            );
            assertEquals("Debt and DTO must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw when DTO is null")
        void shouldThrowWhenDtoIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtMapper.updateEntityFromDto(new Debt(), null)
            );
            assertEquals("Debt and DTO must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw when user not found for new entity")
        void shouldThrowWhenUserNotFoundForNewEntity() {
            Debt debt = new Debt();
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> debtMapper.updateEntityFromDto(debt, validDto)
            );
            assertEquals("User not found with ID: 1", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("toDto Tests")
    class ToDtoTests {

        @Test
        @DisplayName("Should return null for null entity")
        void shouldReturnNullForNullEntity() {
            assertNull(debtMapper.toDto(null));
        }

        @Test
        @DisplayName("Should map entity to DTO with null user")
        void shouldMapEntityToDtoWithNullUser() {
            Debt debt = new Debt();
            debt.setDebtId(1L);
            debt.setDebtName("Credit Card");
            debt.setDebtType(DebtType.CREDIT_CARD);
            debt.setCurrentBalance(new BigDecimal("5000.00"));
            debt.setInterestRate(new BigDecimal("18.99"));

            DebtDTO dto = debtMapper.toDto(debt);

            assertEquals(1L, dto.getDebtId());
            assertNull(dto.getUserId());
            assertNull(dto.getUserName());
            assertEquals("Credit Card", dto.getDebtName());
        }

        @Test
        @DisplayName("Should map entity to DTO with user info")
        void shouldMapEntityToDto() {
            Debt debt = new Debt();
            debt.setDebtId(1L);
            debt.setUser(testUser);
            debt.setDebtName("Credit Card");
            debt.setDebtType(DebtType.CREDIT_CARD);
            debt.setCurrentBalance(new BigDecimal("5000.00"));
            debt.setInterestRate(new BigDecimal("18.99"));

            DebtDTO dto = debtMapper.toDto(debt);

            assertEquals(1L, dto.getDebtId());
            assertEquals(1L, dto.getUserId());
            assertEquals("test@example.com", dto.getUserName());
            assertEquals("Credit Card", dto.getDebtName());
            assertEquals(DebtType.CREDIT_CARD, dto.getDebtType());
        }
    }

    @Nested
    @DisplayName("toEntity Tests")
    class ToEntityTests {

        @Test
        @DisplayName("Should return null for null DTO")
        void shouldReturnNullForNullDto() {
            assertNull(debtMapper.toEntity(null));
        }

        @Test
        @DisplayName("Should create entity from DTO")
        void shouldCreateEntityFromDto() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            Debt debt = debtMapper.toEntity(validDto);

            assertNotNull(debt);
            assertEquals(testUser, debt.getUser());
            assertEquals("Credit Card", debt.getDebtName());
            assertEquals(DebtType.CREDIT_CARD, debt.getDebtType());
        }
    }
}
