# Database Design

Nova uses **PostgreSQL** as the system of record. Schema changes ship exclusively
through **Flyway** migrations under `backend/src/main/resources/db/migration`.

---

## Standards

- **UUID primary keys** for all entities (`gen_random_uuid()` default).
- **snake_case** table and column names.
- **Audit columns** `created_at` / `updated_at` (timestamptz) on every table.
- **Foreign keys** with explicit `ON DELETE` behavior.
- **Indexes** on foreign keys and common filter columns.
- Hibernate runs with `ddl-auto=validate`; Flyway owns the schema.

---

## Phase 1 Schema

### `app_users`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` PK | default `gen_random_uuid()` |
| `email` | `varchar(255)` | unique, not null |
| `full_name` | `varchar(255)` | nullable |
| `created_at` | `timestamptz` | audit |
| `updated_at` | `timestamptz` | audit |

### `categories`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `user_id` | `uuid` FK → `app_users` | `ON DELETE CASCADE` |
| `name` | `varchar(120)` | not null |
| `color` | `varchar(32)` | nullable |
| `icon` | `varchar(64)` | nullable |
| `is_system` | `boolean` | default `false` |
| `created_at` / `updated_at` | `timestamptz` | audit |

Unique: `(user_id, name)`.

### `accounts`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `user_id` | `uuid` FK → `app_users` | `ON DELETE CASCADE` |
| `name` | `varchar(120)` | not null |
| `type` | `varchar(32)` | not null (CHECK via enum in app) |
| `currency` | `varchar(8)` | default `'USD'` |
| `balance` | `numeric(18,4)` | default `0` |
| `is_active` | `boolean` | default `true` |
| `created_at` / `updated_at` | `timestamptz` | audit |

### `transactions`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `user_id` | `uuid` FK → `app_users` | `ON DELETE CASCADE` |
| `account_id` | `uuid` FK → `accounts` | `ON DELETE SET NULL` |
| `category_id` | `uuid` FK → `categories` | `ON DELETE SET NULL` |
| `amount` | `numeric(18,4)` | not null |
| `type` | `varchar(16)` | not null (INCOME/EXPENSE/TRANSFER) |
| `description` | `varchar(255)` | nullable |
| `occurred_at` | `timestamptz` | not null |
| `created_at` / `updated_at` | `timestamptz` | audit |

### `budgets`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `user_id` | `uuid` FK → `app_users` | `ON DELETE CASCADE` |
| `category_id` | `uuid` FK → `categories` | `ON DELETE SET NULL` |
| `name` | `varchar(120)` | not null |
| `amount` | `numeric(18,4)` | not null |
| `period` | `varchar(16)` | not null (DAILY/WEEKLY/MONTHLY/...) |
| `start_date` | `date` | not null |
| `end_date` | `date` | nullable |
| `created_at` / `updated_at` | `timestamptz` | audit |

---

## Indexes

- `idx_categories_user_id`
- `idx_accounts_user_id`
- `idx_transactions_user_id`
- `idx_transactions_account_id`
- `idx_transactions_category_id`
- `idx_transactions_occurred_at`
- `idx_budgets_user_id`

---

## Migration Strategy

- Each change is a new `V{n}__{description}.sql` file.
- Never edit an applied migration; add a new one.
- `baseline-on-migrate` is enabled so connecting to an existing database is safe.
- Entity mappings in `com.nova.*` must stay in sync with the Flyway DDL
  (`ddl-auto=validate` enforces this at startup).
