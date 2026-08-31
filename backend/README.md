# Handmade & Custom Artwork E-Commerce — Backend

IBM Technical Training Capstone Project.

## Stack

| Component       | Technology                                     |
|-----------------|------------------------------------------------|
| Language        | Java 21+ (tested on Java 26.0.1)               |
| Framework       | Spring Boot 3.5.0                              |
| Build           | Apache Maven 3.9+                              |
| Database        | PostgreSQL                                     |
| Migrations      | Flyway (sole schema management authority)      |
| ORM             | Spring Data JPA / Hibernate 6 (`ddl-auto: none`) |
| Security        | Spring Security 6                              |
| Validation      | Jakarta Bean Validation 3                      |
| Testing         | JUnit 5 / Spring Boot Test                     |

## PostgreSQL Prerequisites

The backend requires a running **PostgreSQL** instance.

### 1. Create the application database

```sql
CREATE DATABASE handmade_art_ecommerce;
CREATE USER handmadeart_user WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE handmade_art_ecommerce TO handmadeart_user;
```

For database integration tests, create a separate test database:

```sql
CREATE DATABASE handmade_art_ecommerce_test;
GRANT ALL PRIVILEGES ON DATABASE handmade_art_ecommerce_test TO handmadeart_user;
```

> The application assumes the database already exists. It does **not** attempt to create
> the PostgreSQL database itself. Schema tables are created and managed by Flyway.

### 2. Environment Variables

Configure these before running the backend:

| Variable      | Description                 | Example value                                             |
|---------------|-----------------------------|-----------------------------------------------------------|
| `DB_URL`      | JDBC connection URL         | `jdbc:postgresql://localhost:5432/handmade_art_ecommerce` |
| `DB_USERNAME` | Database username           | `handmadeart_user`                                        |
| `DB_PASSWORD` | Database password           | *(your password)*                                         |

**Never commit real credentials.** Use environment variables or a secrets manager.

**PowerShell:**
```powershell
$env:DB_URL      = "jdbc:postgresql://localhost:5432/handmade_art_ecommerce"
$env:DB_USERNAME = "handmadeart_user"
$env:DB_PASSWORD = "yourpassword"
```

**Bash/Linux/macOS:**
```bash
export DB_URL=jdbc:postgresql://localhost:5432/handmade_art_ecommerce
export DB_USERNAME=handmadeart_user
export DB_PASSWORD=yourpassword
```

## Database Migrations (Flyway)

Flyway is the **sole authority** for schema creation and evolution.

- Migration scripts live in: `src/main/resources/db/migration/`
- Naming convention: `V<version>__<lowercase_description>.sql`  
  Example: `V2__create_identity_tables.sql`
- **Never modify an already-applied migration.** Add new scripts instead.
- Hibernate `ddl-auto` is set to `none` permanently. Flyway manages all schema changes.

Flyway runs automatically on application startup. On first run against a new database
it creates the `flyway_schema_history` table and applies all pending migrations.

To run migrations manually (without starting the full application):
```bash
mvn flyway:migrate -Dflyway.url=$DB_URL -Dflyway.user=$DB_USERNAME -Dflyway.password=$DB_PASSWORD
```

## How to Run the Backend

```bash
# Set environment variables first (see above), then from the backend/ directory:
mvn spring-boot:run
```

The server starts on port **8080**.

## How to Run Tests

### Default test suite (no database required)

```bash
mvn clean test
```

Runs the Spring context load test and all unit/non-DB tests.
DataSource and Flyway auto-configuration are excluded from this profile.

### Database integration tests (requires PostgreSQL)

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` to point at `handmade_art_ecommerce_test`, then:

```bash
mvn clean test -Dgroups=db-integration -Dspring.profiles.active=db-integration
```

These tests verify:
- DataSource creation
- Live PostgreSQL connection
- Flyway `schema_history` table creation
- V1 baseline migration applied successfully

## Package Structure

```
com.handmadeart.ecommerce
├── config        — Spring configuration (security, beans, CORS, …)
├── controller    — REST controllers
├── dto
│   ├── request   — Inbound API request DTOs
│   └── response  — Outbound API response DTOs
├── entity        — JPA entities (Phase 2B+)
├── exception     — Custom exceptions and global handler
├── repository    — Spring Data JPA repositories (Phase 2B+)
├── security      — JWT filters, auth providers, security utilities (Phase 3)
├── service       — Business logic layer
└── util          — Shared utility / helper classes
```

## Development Phases

See `project-docs/DEVELOPMENT_STATUS.md` for current phase and next tasks.
