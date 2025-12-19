package com.jameselner.finance_hub.domain;

import com.jameselner.finance_hub.domain.enums.HousingExpenseType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "housing_expenses",
    indexes = {
        @Index(name = "idx_housing_expenses_user_id", columnList = "user_id"),
        @Index(name = "idx_housing_expenses_user_month", columnList = "user_id, expense_month")
    }
)
public class HousingExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    private Long expenseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_housing_expenses_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_type", nullable = false, length = 50)
    private HousingExpenseType expenseType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_month", nullable = false)
    private LocalDate expenseMonth;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
