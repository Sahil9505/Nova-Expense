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
| `category_type` | `varchar(16)` | not null, default `'EXPENSE'` (`INCOME`, `EXPENSE`) |
| `color` | `varchar(32)` | nullable |
| `icon` | `varchar(64)` | nullable |
| `is_system` | `boolean` | default `false` |
| `created_at` / `updated_at` | `timestamptz` | audit |

Unique: `(user_id, name, category_type)` — a user may have both an "Other"
income and an "Other" expense category. System categories are seeded per user on
registration and cannot be deleted.

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
| `institution` | `varchar(120)` | nullable |
| `color` | `varchar(32)` | nullable |
| `icon` | `varchar(64)` | nullable |
| `created_at` / `updated_at` | `timestamptz` | audit |

`type` is the enum `CASH`, `CHECKING`, `SAVINGS`, `CREDIT_CARD`, `WALLET`.
"Deleting" an account is a soft deactivation (`is_active = false`); its transaction
history is preserved.

### `transactions`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `user_id` | `uuid` FK → `app_users` | `ON DELETE CASCADE` |
| `account_id` | `uuid` FK → `accounts` | source account; `ON DELETE SET NULL` |
| `destination_account_id` | `uuid` FK → `accounts` | transfer target; `ON DELETE SET NULL` |
| `category_id` | `uuid` FK → `categories` | `ON DELETE SET NULL` |
| `amount` | `numeric(18,4)` | not null, always positive |
| `type` | `varchar(16)` | not null (`INCOME`, `EXPENSE`, `TRANSFER`) |
| `merchant` | `varchar(255)` | nullable |
| `description` | `varchar(255)` | nullable (the transaction note) |
| `currency` | `varchar(8)` | not null, default `'USD'` |
| `tags` | `varchar(255)` | nullable |
| `occurred_at` | `timestamptz` | not null |
| `created_at` / `updated_at` | `timestamptz` | audit |

The `amount` is stored positive; direction is implied by `type`. For `TRANSFER`,
`account_id` is the source and `destination_account_id` is the target; `category_id`
is `null`. For `INCOME`/`EXPENSE`, `destination_account_id` is `null` and
`category_id` must match the transaction type. Account balances are kept in sync
with transactions by the transaction service.

### `budgets`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `user_id` | `uuid` FK → `app_users` | `ON DELETE CASCADE` |
| `category_id` | `uuid` FK → `categories` | `ON DELETE SET NULL` |
| `name` | `varchar(120)` | not null |
| `amount` | `numeric(18,4)` | not null |
| `period` | `varchar(16)` | not null (WEEKLY/MONTHLY/YEARLY/CUSTOM) |
| `description` | `varchar(255)` | nullable |
| `is_active` | `boolean` | not null, default `true` |
| `start_date` | `date` | not null |
| `end_date` | `date` | nullable (required for CUSTOM) |
| `created_at` / `updated_at` | `timestamptz` | audit |

---

## Indexes

- `idx_categories_user_id`
- `idx_accounts_user_id`
- `idx_accounts_type`
- `idx_categories_type`
- `idx_transactions_user_id`
- `idx_transactions_account_id`
- `idx_transactions_category_id`
- `idx_transactions_type`
- `idx_transactions_destination_account_id`
- `idx_transactions_occurred_at`
- `idx_budgets_user_id`
- `idx_budgets_active` (Phase 4A: V4)

---

## Phase 3 Schema (V3)

V3 evolves the Phase 1 foundation for real expense tracking. V1/V2 are never
edited; every change ships here so production uses `ddl-auto=validate` against this
Flyway-managed schema.

- `accounts` gains `institution`, `color`, and `icon` columns; legacy Phase 1
  account types (`CREDIT`, `INVESTMENT`) are remapped to the Phase 3 enum
  (`CREDIT_CARD`, `SAVINGS`).
- `categories` gains `category_type` (default `'EXPENSE'`) and the uniqueness
  constraint becomes `(user_id, name, category_type)`.
- `transactions` gains `merchant`, `currency`, `tags`, and
  `destination_account_id`; `currency` defaults to `'USD'`.

No new tables are introduced in V3 — the `budgets` table from Phase 1 remains the
foundation for **Phase 4** (budgets are not yet exposed via the API).

---

## Phase 2 Schema (V2)

V2 extends `app_users` and adds the `refresh_tokens` table. V1 is never edited;
all changes ship as new migrations.

### `app_users` (extended)

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` PK | (Phase 1) |
| `email` | `varchar(255)` | unique, not null (Phase 1) |
| `full_name` | `varchar(255)` | nullable (Phase 1) |
| `password_hash` | `varchar(255)` | BCrypt hash; never returned to clients |
| `role` | `varchar(32)` | not null, default `'USER'` (`USER`, `ADMIN`) |
| `account_status` | `varchar(32)` | not null, default `'ACTIVE'` (`ACTIVE`, `DISABLED`, `LOCKED`, `PENDING`) |
| `preferred_currency` | `varchar(8)` | not null, default `'USD'` |
| `timezone` | `varchar(64)` | nullable |
| `avatar_url` | `varchar(512)` | nullable |
| `last_login_at` | `timestamptz` | nullable, set on login |
| `created_at` / `updated_at` | `timestamptz` | audit (Phase 1) |

### `refresh_tokens`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `user_id` | `uuid` FK → `app_users` | `ON DELETE CASCADE` |
| `token_hash` | `varchar(255)` | SHA-256 hash of the issued raw token (the raw value is never stored) |
| `expires_at` | `timestamptz` | not null |
| `revoked` | `boolean` | not null, default `false` |
| `replaced_by` | `uuid` | nullable; points at the token that replaced this one after rotation |
| `created_at` / `updated_at` | `timestamptz` | audit |

Unique: `token_hash`. Indexes: `(user_id)`, `(token_hash)`.

---

## Migration Strategy

- Each change is a new `V{n}__{description}.sql` file.
- Never edit an applied migration; add a new one.
- `baseline-on-migrate` is enabled so connecting to an existing database is safe.
- Entity mappings in `com.nova.*` must stay in sync with the Flyway DDL
  (`ddl-auto=validate` enforces this at startup).
