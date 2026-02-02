# Finance Hub

Personal finance management application for tracking accounts, budgets, transactions, debts, savings goals, income, and expenses.

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA
- **Frontend**: Vaadin 24 (Java-based UI) with React components
- **Database**: PostgreSQL 16 with Flyway migrations
- **Build**: Maven

## Project Structure

```
src/main/java/com/jameselner/finance_hub/
├── domain/           # JPA entities + enums
├── dto/              # Data transfer objects
├── mapper/           # Entity-DTO mappers
├── repository/       # Spring Data JPA repositories
├── service/          # Business logic (accepts/returns DTOs)
├── security/         # Auth & security components
├── view/             # Vaadin views
└── view/components/  # Reusable UI components (dialogs, forms)
```

## Commands

```bash
# Run locally (requires PostgreSQL via docker-compose)
docker-compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Run tests
./mvnw test

# Production build
./mvnw clean package -Pprod
```

## Code Patterns

### Layered Architecture
Views → Services → Repositories → Entities

### Naming
- Entities: `Account`, `Transaction`
- DTOs: `AccountDTO`, `TransactionDTO`
- Mappers: `AccountMapper`
- Services: `AccountService`
- Views: `AccountsView`
- Components: `AccountFormDialog`

### Entity Pattern
```java
@Entity
@Table(name = "accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
```

### Service Pattern
- All business logic in services
- `@Transactional(readOnly = true)` for queries
- Services accept/return DTOs, not entities

## Domain Model

Core entities: User, Account, Transaction, Category, Budget, Debt, DebtPayment, IncomeSource, IncomeDeduction, SavingsGoal, SavingsGoalContribution, Subscription, HousingExpense, Holiday, HolidayExpense, UserSettings

Key enums: AccountType, TransactionType, CategoryType, DebtType, DeductionType, PeriodType, Priority

## Database

- Migrations in `src/main/resources/db/migration/` (V1-V16)
- Table names: lowercase, plural (`accounts`, `transactions`)
- Column names: snake_case (`user_id`, `created_at`)
- All entities have `createdAt`/`updatedAt` timestamps
- Soft deletes used for debts (`deleted` flag)

## Security

- Session-based authentication (not JWT)
- BCrypt password hashing
- All user data filtered by `userId`
- Input sanitization via `InputSanitizer`
