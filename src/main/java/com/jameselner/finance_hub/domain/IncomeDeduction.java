package com.jameselner.finance_hub.domain;

import com.jameselner.finance_hub.domain.enums.DeductionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "income_deductions",
        indexes = {
                @Index(name = "idx_income_deductions_income_source_id", columnList = "income_source_id"),
                @Index(name = "idx_income_deductions_type", columnList = "deduction_type")
        }
)
public class IncomeDeduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deduction_id")
    private Long deductionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "income_source_id", nullable = false, foreignKey = @ForeignKey(name = "fk_income_deductions_source"))
    private IncomeSource incomeSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "deduction_type", nullable = false, length = 50)
    private DeductionType deductionType;

    @Column(name = "deduction_name", nullable = false, length = 255)
    private String deductionName;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "is_percentage", nullable = false)
    private Boolean isPercentage = false;

    @Column(name = "percentage_value", precision = 5, scale = 2)
    private BigDecimal percentageValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
