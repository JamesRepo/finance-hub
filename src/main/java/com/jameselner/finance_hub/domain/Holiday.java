package com.jameselner.finance_hub.domain;

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
@Table(name = "holidays",
    indexes = {
        @Index(name = "idx_holidays_user_id", columnList = "user_id"),
        @Index(name = "idx_holidays_start_date", columnList = "start_date")
    }
)
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_id")
    private Long holidayId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_holidays_user"))
    private User user;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "destination", nullable = false, length = 200)
    private String destination;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "budget", nullable = false, precision = 15, scale = 2)
    private BigDecimal budget;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "holiday", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HolidayExpense> expenses = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Helper method to add an expense to this holiday
     */
    public void addExpense(final HolidayExpense expense) {
        expenses.add(expense);
        expense.setHoliday(this);
    }

    /**
     * Helper method to remove an expense from this holiday
     */
    public void removeExpense(final HolidayExpense expense) {
        expenses.remove(expense);
        expense.setHoliday(null);
    }
}