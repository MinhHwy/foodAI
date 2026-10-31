# FoodAI

FoodAI is a web application with a static frontend, a Spring Boot persistence backend, and a MySQL database. The database schema is maintained in [`Database/FoodAI_Consolidated_Database.sql`](Database/FoodAI_Consolidated_Database.sql).

## Database setup

- Use MySQL 8.0.16 or later. This schema uses MySQL `ENUM`, `JSON`, `DATETIME(3)`, enforced `CHECK` constraints, and the `utf8mb4_0900_ai_ci` collation.
- The database name is `foodai`. The SQL file creates it if it is missing, defines its tables, and includes starter food data.
- For a new, empty local database, import the SQL file once with MySQL Workbench (open the SQL file and execute it) or from PowerShell:

  ```powershell
  cmd.exe /c "mysql -u root -p < Database\FoodAI_Consolidated_Database.sql"
  ```

- Do not re-import this file into an existing database: its starter `INSERT` statements are not intended to be rerun and the application must not initialize or replace existing data.
- Verify the imported schema with:

  ```powershell
  mysql -u root -p -D foodai -e "SHOW TABLES;"
  ```

Each developer uses their own MySQL instance and credentials. Local database changes do not synchronize across team members. Keep the SQL schema in source control; distribute future schema changes through an agreed SQL update or a migration process.

## Backend configuration

The backend requires JDK 21 and uses the checked-in Maven Wrapper. From the project root in PowerShell, configure the local connection and run the tests:

```powershell
Set-Location .\backend
$env:DB_URL = "jdbc:mysql://localhost:3306/foodai?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<your-local-mysql-password>"
.\mvnw.cmd test
```

`DB_URL` defaults to the local `foodai` URL and `DB_USERNAME` defaults to `root`; `DB_PASSWORD` is required and is not stored in project files. Set each variable in the same PowerShell session before running `.\mvnw.cmd spring-boot:run` or `.\mvnw.cmd package`. Do not commit local credentials.

The application uses `spring.jpa.hibernate.ddl-auto=validate`: startup checks that the database matches the entity mappings and does not create, update, or delete schema objects. Hibernate SQL logging is enabled for development.

Common connection checks:

- Confirm the MySQL service is running and listening on port 3306.
- Confirm the `foodai` database and all tables have been imported.
- Check `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the current PowerShell session.
- A Hibernate schema validation error means the existing SQL schema and mappings differ; fix the mapping or propose a reviewed SQL migration rather than changing `ddl-auto` to `update`.

## Persistence mapping

All 18 schema tables have JPA entity and Spring Data repository mappings. Foreign keys are represented with lazy relationships; no cascading writes or deletes are configured. The two tables whose primary key is also a user foreign key use `@MapsId`. MySQL `DATETIME(3)` fields map to `LocalDateTime`, `DATE` to `LocalDate`, exact decimal values to `BigDecimal`, and JSON fields to maps.

The main relationships follow the SQL foreign keys: users own their profiles, settings, goals, weight logs, recognition sessions, diary meals, and recommendation runs; foods link to categories, aliases, model classes, recognition predictions/corrections, diary snapshots, and recommendations; recognition sessions contain recognition items, diary meals contain diary items, and recommendation runs contain recommendation items. Nullable foreign keys remain optional. Relationships are deliberately unidirectional to avoid unnecessary loading and JSON cycles. No ERD file was present, so the SQL file remains the schema source of truth; its indexes, unique keys, defaults, and check constraints are preserved without duplicating them in Hibernate DDL.

| Database table | Entity | Repository |
|---|---|---|
| `users` | `User` | `UserRepository` |
| `auth_codes` | `AuthCode` | `AuthCodeRepository` |
| `user_profiles` | `UserProfile` | `UserProfileRepository` |
| `user_settings` | `UserSettings` | `UserSettingsRepository` |
| `user_goals` | `UserGoal` | `UserGoalRepository` |
| `weight_logs` | `WeightLog` | `WeightLogRepository` |
| `food_categories` | `FoodCategory` | `FoodCategoryRepository` |
| `foods` | `Food` | `FoodRepository` |
| `food_aliases` | `FoodAlias` | `FoodAliasRepository` |
| `model_food_classes` | `ModelFoodClass` | `ModelFoodClassRepository` |
| `recognition_sessions` | `RecognitionSession` | `RecognitionSessionRepository` |
| `recognition_items` | `RecognitionItem` | `RecognitionItemRepository` |
| `recognition_feedback` | `RecognitionFeedback` | `RecognitionFeedbackRepository` |
| `diary_meals` | `DiaryMeal` | `DiaryMealRepository` |
| `diary_items` | `DiaryItem` | `DiaryItemRepository` |
| `recommendation_runs` | `RecommendationRun` | `RecommendationRunRepository` |
| `recommendation_items` | `RecommendationItem` | `RecommendationItemRepository` |
| `audit_logs` | `AuditLog` | `AuditLogRepository` |

The mappings preserve the SQL ENUM values and the schema's primary-key types. Password hashes and authentication-code hashes are persistence fields only; do not return these entities directly from REST endpoints. No controllers, authentication flow, business services, or migrations are added by this database integration.