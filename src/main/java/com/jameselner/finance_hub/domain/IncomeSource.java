package com.jameselner.finance_hub.domain;

import com.jameselner.finance_hub.domain.enums.RecurrenceFrequency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "income_sources",
        indexes = {
                @Index(name = "idx_income_sources_user_id", columnList = "user_id"),
                @Index(name = "idx_income_sources_active", columnList = "is_active"),
                @Index(name = "idx_income_sources_recurring", columnList = "is_recurring"),
                @Index(name = "idx_income_sources_next_date", columnList = "next_expected_date")
        }
)
public class IncomeSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "income_source_id")
    private Long incomeSourceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_income_sources_user"))
    private User user;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "net_amount", precision = 15, scale = 2)
    private BigDecimal netAmount;

    @OneToMany(mappedBy = "incomeSource", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IncomeDeduction> deductions = new ArrayList<>();

    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency", length = 20)
    private RecurrenceFrequency recurrenceFrequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_expected_date")
    private LocalDate nextExpectedDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_income_sources_category"))
    private Category category;

    @Column(name = "auto_create_transaction", nullable = false)
    private Boolean autoCreateTransaction = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
