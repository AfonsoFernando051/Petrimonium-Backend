# Backend module & schema boundary plan

Status as of 2026-08-31: **§5-6 (schema-per-context) and JWT app_context +
BFF enforcement (tasks 6 and 3 of the original ecosystem-onboarding prompt)
executed and verified.** §3-4 of *this* doc (package restructuring, ArchUnit
boundary test) still not started — you chose the app_context/BFF work first
when asked. You said "assuma e aja" on the 4 open decisions in §7 (all
resolved as recommended) and I proceeded with the schema split — see §9 for
what actually landed there. See §10 for the app_context/BFF work.

## 9. What's actually done (2026-08-31)

- Every JPA entity now declares its target schema (`@Table(..., schema =
  "...")`) per the §5 mapping.
- **§6's `ALTER TABLE ... SET SCHEMA` migration turned out to be
  PostgreSQL-only** — confirmed empirically (not by reading docs) that H2
  rejects a cross-schema `ALTER TABLE ... RENAME`: `"Schema name must
  match"`. This only surfaced because the dev profile (`application-dev.properties`)
  boots against H2 too, via Flyway, and I smoke-tested that boot rather than
  assuming the migration was portable like every prior one in this repo. Fix:
  - `V20__schema_separation.sql` moved to a new `db/migration-postgres/`
    location, wired into `spring.flyway.locations` for the `prod` profile
    only (`application-prod.properties`).
  - Dev keeps `db/migration` + `db/migration-dev`, unchanged — its tables
    stay physically unqualified in H2's default schema, exactly as before.
  - New `DevSchemalessNamingStrategy` (`infrastructure/config/`), wired via
    `spring.jpa.properties.hibernate.physical_naming_strategy` in
    `application-dev.properties` only — makes Hibernate's `ddl-auto=validate`
    ignore entities' declared `schema` in dev, so it agrees with what Flyway
    actually built there. Extends Hibernate's own
    `PhysicalNamingStrategySnakeCaseImpl` (confirmed as Spring Boot 4.0.1's
    actual default via its autoconfigure source, since the old
    `SpringPhysicalNamingStrategy` class no longer exists in this Boot
    version) — first attempt extended the wrong base class and broke
    `isActive` → `is_active` column resolution; caught by rebooting dev
    again rather than assuming the fix worked.
  - Test profile needed one addition —
    `spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true` — so
    `create-drop` issues `create schema if not exists` for H2 too. This one
    worked on the first try.
- **Verified, not just written:** full `mvn test` (821/821, unchanged) run
  twice (before and after the H2 fix), plus two real `spring-boot:run` boots
  under the `dev` profile watching for the actual Flyway/Hibernate log
  output, not just a clean exit code.
- **Not yet done:** §3 (package restructuring into
  `com.jf.PetApp.<context>/...`) and §4 (ArchUnit boundary test) — these
  don't touch the database and carry a very different risk profile (a
  ~250-file mechanical rename, hard to review as one diff, versus the
  schema split's small, DB-tested change). Flagging before starting rather
  than folding it into the same pass silently.

## 0. The one structural decision this plan makes: package modules, not Maven multi-module

The original prompt asks for "package names, module dependencies, and... a
Gradle/Maven restructuring proposal." Two ways to satisfy that:

| Option | What it means | Trade-off |
|---|---|---|
| **A — Package-per-context, one Maven module (recommended)** | Keep one `pom.xml`, one JAR. Restructure packages so each bounded context owns its own `application`/`core`/`infrastructure` slice, and add an ArchUnit test that fails the build on an illegal cross-context import. | Zero build/CI/deploy complexity added. Fully reversible, fast to do incrementally (context by context). The boundary is enforced by a test, not by the compiler — a determined developer *could* violate it, but the ArchUnit gate catches it at `mvn test`, same rigor as the mutation-testing gate already in this `pom.xml`. |
| **B — True Maven multi-module (separate `pom.xml` per context)** | Compiler-enforced boundaries — a context literally cannot import another's internals unless it's an exported API. | More CI/build machinery (multi-module reactor, per-module versioning), slower local iteration, and for a single-maintainer repo that's explicitly *not* extracting services yet (§1.1: "keep one backend platform"), the extra rigor doesn't buy much beyond what an ArchUnit test already buys. |

**Recommendation: A.** It gets you everything §1.1 actually asks for ("so a
future service extraction is cheap") without the overhead, and it matches
"no new infrastructure... unless explicitly ask" from your constraints
section. Revisit B only if/when a context is actually being extracted to its
own deployable — not preemptively. Flag if you'd rather have B from the
start.

## 1. Current table inventory → target context (full audit)

Every table that exists today (from `V1`–`V19`), single `public` schema, no
separation:

| Table | → Context | Notes |
|---|---|---|
| `jf_users` | `identity` | |
| `jf_password_reset_tokens` | `identity` | |
| `jf_refresh_tokens` | `identity` | |
| `academy_domains`, `academy_domain_translations` | `education` | |
| `academy_schools`, `academy_school_translations`, `academy_school_prerequisites` | `education` | |
| `learning_modules`, `academy_module_translations`, `academy_module_prerequisites` | `education` | `learning_modules` is the authoritative id+XP catalog (V4/V9), extended by `academy_*` (V10) — same context, not a legacy table to retire |
| `learning_lessons`, `academy_lesson_translations` | `education` | same relationship as above |
| `academy_lesson_steps`, `academy_lesson_step_translations` | `education` | |
| `academy_choice_question_options`, `academy_choice_question_option_translations` | `education` | |
| `academy_lesson_step_takeaways`, `academy_lesson_step_takeaway_translations` | `education` | |
| `academy_lesson_portfolio_concepts` | `education` | Tags a lesson with a portfolio-indicator concept id (`pe`, `dy`, `roe`) for the client's "you just learned X" callback — **checked and confirmed this is a content tag, not a financial-data FK**: it only references `learning_lessons`, never `jf_investments`. Safe to keep in `education`. |
| `lesson_progress` | `education` | Per-user completion state, FK'd to `learning_lessons` — kept with its content rather than `gamification`, since it's "did they finish X," not an XP/streak ledger |
| `jf_finances`, `jf_investments` | `real_portfolio` | **See §2 — this is the one real judgment call in this plan** |
| `xp_events`, `achievement_unlocks`, `activity_log`, `mission_completions` | `gamification` | |
| `jf_pets` | `pet` | |
| `jf_mentor_conversations`, `jf_mentor_messages` | `ai` | |

Every table above has a `user_id` FK to `jf_users` — that becomes a
cross-schema FK once `identity` moves to its own schema (see §5; Postgres
allows this natively, same database instance).

## 2. Resolving the `investment` → `real_portfolio` ambiguity from the audit

The audit flagged that `jf_investments`/`jf_finances` have no real/simulated
marker. Having now read every migration, here's the concrete read:

- `jf_investments`/`jf_finances` (V1) store **user-entered, real-money
  investment lots** — ticker, quantity, purchase price/date — enriched live
  against `brapi.dev` market data. There is no "simulated" flavor of this
  anywhere in the schema.
- The only genuinely *simulated* financial construct in this backend is the
  **Financial Lab** (`application/lab/**`, `SimulatorCatalog`) — and it
  stores **no monetary state at all**. Completing a simulator only writes an
  XP-ledger row (`xp_events`, `event_type = 'LESSON_COMPLETED'`-shaped via
  `V19`'s new simulator event type). There's no `simulated_portfolio` table
  today because nothing has ever needed one — the Lab is practice
  interaction, not a persisted paper portfolio.

**Proposed resolution:** `jf_investments`/`jf_finances` become `real_portfolio`,
owned by the Wallet BFF only. No `simulated_portfolio` schema/tables are
created in this pass — there's nothing to migrate into them yet. If Academy
later needs a persisted simulated portfolio (not just Lab completion XP),
that's new tables designed fresh against Academy's actual requirements, not
a split of `jf_investments`.

**What this means for the Academy Flutter repo (flagging for you, not
deciding on their behalf):** `petrimonium-academy`'s own audit noted its
`features/portfolio`/`features/investment`/`features/asset_details` are
still present and still call this same backend's `/api/investments`
endpoints. Once `real_portfolio` is Wallet-scoped at the BFF layer (task 3),
Academy would lose access to real investment data through the API — which is
the intended enforcement, but it's a breaking change for Academy's *current*
code that the Academy session should know is coming.

## 3. Package layout proposal

Current: package-by-layer-then-feature (`application/<feature>`,
`core/domain/<Thing>`, `infrastructure/entity/<Thing>JpaEntity`,
`infrastructure/controller/<feature>`) — the feature is a leaf under each
layer, not the top-level grouping.

Proposed: package-by-context, with the existing hexagonal layers *inside*
each context package (same `application`/`core`/`infrastructure` split you
already use, just nested one level differently):

```
com.jf.PetApp.identity/          (auth, User, JWT, password reset, refresh tokens,
                                   onboarding, InvestorProfile — see below)
    application/  core/  infrastructure/
com.jf.PetApp.education/         (academy catalog, learning progress)
    application/  core/  infrastructure/
com.jf.PetApp.realportfolio/     (investment, finance)
    application/  core/  infrastructure/
com.jf.PetApp.gamification/      (xp, achievements, missions, streaks)
    application/  core/  infrastructure/
com.jf.PetApp.pet/               (pet state)
    application/  core/  infrastructure/
com.jf.PetApp.ai/                (mentor)
    application/  core/  infrastructure/
com.jf.PetApp.shared/            (SecurityUtils, GlobalExceptionHandler,
                                   DotenvLoader, HttpClientConfig, RequestIdFilter,
                                   RateLimitingFilter, translation)
```

`onboarding*`: **resolved — `identity`.** The investor-profile questionnaire
computes `InvestorProfile`, which today lives on `User` itself and is shared
by both apps from first login — a user attribute, not curriculum, even though
the question content reads that way. Decision made 2026-08-31, no longer a
judgment call.

This is a mechanical, IDE-assisted rename across the ~250 files in
`src/main/java` — no logic changes. Proposed execution order (once approved):
one context per commit (`identity` first, since everything else depends on
it), tests green after each, not one giant rename commit.

## 4. Module dependency rules (the ArchUnit gate from §0)

- **`identity`**: depends on nothing else in this list. Every other context
  depends on it (for `user_id` resolution) but never the reverse.
- **`education`**, **`realportfolio`**, **`pet`**: each depends only on
  `identity` + `shared`. They must never import each other's `core`/
  `infrastructure` packages directly.
- **`gamification`**: depends on `identity` + `shared`. It must **not**
  import `realportfolio` or `education` internals — it already only takes
  explicit commands/DTOs today (`XpLedgerService`, `AchievementContext`,
  `MissionContext`), which is the right shape to enforce, not retrofit.
- **`ai`** (mentor): depends on `identity` + `shared`. Reads other contexts'
  data only through explicit context passed in (`MentorClientContextDTO`),
  never direct repository access into `education`/`realportfolio`.
- **`pet`**: depends on `identity` + `shared` today (state CRUD only). Once
  the signal-ingestion API is designed (task 5), it will additionally accept
  explicit signal DTOs from `education`/`realportfolio` — never raw entities,
  never (for the Wallet path) price/return fields.

An ArchUnit test (`mvn test`-gated, same pattern as the existing PIT
mutation-coverage gate in `pom.xml`) would assert these import rules
directly — proposed as part of the execution, not written yet.

## 5. Schema-per-context plan (same Postgres instance, six schemas)

```
identity        ← jf_users, jf_password_reset_tokens, jf_refresh_tokens
education       ← academy_domains, academy_domain_translations, academy_schools,
                   academy_school_translations, academy_school_prerequisites,
                   learning_modules, academy_module_translations, academy_module_prerequisites,
                   learning_lessons, academy_lesson_translations,
                   academy_lesson_steps, academy_lesson_step_translations,
                   academy_choice_question_options, academy_choice_question_option_translations,
                   academy_lesson_step_takeaways, academy_lesson_step_takeaway_translations,
                   academy_lesson_portfolio_concepts, lesson_progress
real_portfolio  ← jf_finances, jf_investments
gamification    ← xp_events, achievement_unlocks, activity_log, mission_completions
pet             ← jf_pets
ai              ← jf_mentor_conversations, jf_mentor_messages
```

Cross-schema FKs (every `user_id` column → `identity.jf_users`) are legal and
cheap in Postgres within one instance — no denormalization, no duplicated
user table needed.

## 6. Migration plan — SQL diff for review, **not executed**

The key fact that makes this low-risk: Postgres's `ALTER TABLE ... SET SCHEMA`
is a **metadata-only catalog operation** — no data is rewritten, no rows are
copied, it's near-instant regardless of table size, and existing FKs/indexes
survive automatically since they still point at the same physical table.
This is exactly the "feasible without a risky migration" case the original
prompt asked to look for.

Proposed `V20__schema_separation.sql` (illustrative — would be reviewed
again as an actual Flyway file before running):

```sql
-- Additive schema creation, then metadata-only table moves. No data rewrite,
-- no downtime beyond a brief DDL lock per statement.

create schema if not exists identity;
create schema if not exists education;
create schema if not exists real_portfolio;
create schema if not exists gamification;
create schema if not exists pet;
create schema if not exists ai;

alter table jf_users set schema identity;
alter table jf_password_reset_tokens set schema identity;
alter table jf_refresh_tokens set schema identity;

alter table academy_domains set schema education;
alter table academy_domain_translations set schema education;
alter table academy_schools set schema education;
alter table academy_school_translations set schema education;
alter table academy_school_prerequisites set schema education;
alter table learning_modules set schema education;
alter table academy_module_translations set schema education;
alter table academy_module_prerequisites set schema education;
alter table learning_lessons set schema education;
alter table academy_lesson_translations set schema education;
alter table academy_lesson_steps set schema education;
alter table academy_lesson_step_translations set schema education;
alter table academy_choice_question_options set schema education;
alter table academy_choice_question_option_translations set schema education;
alter table academy_lesson_step_takeaways set schema education;
alter table academy_lesson_step_takeaway_translations set schema education;
alter table academy_lesson_portfolio_concepts set schema education;
alter table lesson_progress set schema education;

alter table jf_finances set schema real_portfolio;
alter table jf_investments set schema real_portfolio;

alter table xp_events set schema gamification;
alter table achievement_unlocks set schema gamification;
alter table activity_log set schema gamification;
alter table mission_completions set schema gamification;

alter table jf_pets set schema pet;

alter table jf_mentor_conversations set schema ai;
alter table jf_mentor_messages set schema ai;
```

**Also needed alongside this migration (not just the SQL):**

- Every JPA entity gets an explicit `@Table(name = "...", schema = "...")` —
  currently no entity specifies a schema, so Hibernate validates against
  whatever `search_path`/default schema is active. This has to land in the
  same change as the migration, or `ddl-auto=validate` starts failing on
  boot.
- `spring.flyway.schemas=identity,education,real_portfolio,gamification,pet,ai`
  — Flyway needs to know about all six so it can create them and track
  history correctly. The `flyway_schema_history` table itself stays wherever
  Flyway's default/first-listed schema puts it (typically `public` or the
  first schema in the list) — one history table for the whole database is
  fine; schema-per-context is about data tables, not migration bookkeeping.
- H2 (dev profile) needs the same schema names to exist for
  `ddl-auto=validate` to pass locally — H2 supports `CREATE SCHEMA` the same
  way, so this is additive there too.

## 7. Decisions I need from you before any of this executes

1. **Package modules (A) vs. true Maven multi-module (B)** — recommending A (§0).
2. **`jf_investments`/`jf_finances` → `real_portfolio`, Wallet-owned, no
   `simulated_portfolio` tables created yet** (§2) — confirming this reading
   is correct, since it's the one place the data itself doesn't declare its
   own answer.
3. ~~`onboarding`/`InvestorProfile` → `identity` vs. `education`~~ (§3) —
   **resolved 2026-08-31: `identity`.**
4. **Schema names** (`identity`, `education`, `real_portfolio`,
   `gamification`, `pet`, `ai`) — cheap to bikeshed now, expensive to rename
   once migrations reference them.

## 8. What happens after approval (not started)

1. `V20__schema_separation.sql` + entity `@Table(schema=...)` annotations +
   Flyway config, run against dev first, tests green, then prod.
2. Package rename, one context at a time, starting with `identity`.
3. ArchUnit boundary test added to the `mvn test` gate.

None of this touches the BFF layer, the gamification allow-list, the Pet
signal API, or the JWT claims contract — those are tasks 3–6, unstarted,
after this one is approved and landed.

## 10. JWT app_context claim + BFF enforcement (2026-08-31)

What landed, following the `app_context` shape already published to Academy
in `petrimonium-academy/docs/CROSS_REPO_CONTRACTS.md` §1:

- **`AppContextEnum`** (`core/domain/enums/`) — `ACADEMY`/`WALLET`. Owns the
  claim-value/authority-name mapping in one place (`claimValue()`,
  `authority()`, `fromClaimValue()` for trusted JWT parsing,
  `fromRequestValue()` for untrusted client input — see below).
- **Request contract**: `/auth/login` and `/auth/google` accept an optional
  `appContext` field ("academy"/"wallet", case-insensitive). Absent/blank is
  allowed — the resulting token simply carries no `app_context` claim (safe
  default, matches every client that hasn't adopted this yet). An
  unrecognized non-blank value is a 400 `INVALID_REQUEST`
  (`AppContextEnum.fromRequestValue`, reusing the existing
  `IllegalArgumentException` → `GlobalExceptionHandler` mapping — no new
  exception type needed).
- **Token contract**: `TokenProvider.generateToken(User, AppContextEnum)`
  embeds `"app_context": "academy"|"wallet"` (lowercase, per the published
  shape) only when non-null. `/auth/refresh` does **not** accept an
  `appContext` field — the rotated token always inherits the value stored on
  the refresh-token row being rotated (`RefreshToken.appContext`, new nullable
  column, `V21__add_app_context_to_refresh_tokens.sql`). This was a
  deliberate call beyond what the contract doc specified: letting a refresh
  request choose its own context would let a stolen Academy-context refresh
  token mint a Wallet-context access token, defeating the whole point. A
  session's app scope is fixed at login/google-login and can only change by
  authenticating again.
- **Enforcement (the actual BFF)**: `JwtAuthenticationFilter` grants an
  `APP_CONTEXT_WALLET`/`APP_CONTEXT_ACADEMY` Spring Security authority
  alongside the existing `ROLE_*` one, when the token carries the claim.
  `SecurityConfig` gates `/api/investments/**` behind
  `hasAuthority(APP_CONTEXT_WALLET)` and `/api/v1/academy/**`,
  `/api/v1/learning/**`, `/api/v1/lab/**` behind
  `hasAuthority(APP_CONTEXT_ACADEMY)`. Everything else (gamification, pet,
  mentor, onboarding, settings, auth) stays `anyRequest().authenticated()` —
  unrestricted by app_context, per the ECOSYSTEM.md audit (those contexts
  have no real/simulated leakage risk today).
- **Confirmed breaking change, as flagged in ECOSYSTEM.md**: a token with no
  `app_context` (any token minted before this change, or a client that
  hasn't adopted the field yet) can no longer reach `/api/investments/**` —
  this was called out in advance as the intended enforcement, not a
  regression. Academy's Flutter repo needs to either start sending
  `appContext: "wallet"` at login for its portfolio screens, or (more
  likely, per the ECOSYSTEM.md note) drop those screens once Wallet exists
  as their real home.
- **Verified, not just written**: full `mvn test`, 831/831 (821 + 10 new
  tests covering claim round-trip, filter authority-granting, and the
  BFF-enforcement paths — both the 403 and the 200 sides — in
  `SecurityConfigTest`'s real filter-chain slice, not a mocked one). Also
  smoke-booted `dev` profile against H2 and confirmed
  `V21__add_app_context_to_refresh_tokens.sql` actually applies
  (`Migrating schema "PUBLIC" to version "21 - add app context to refresh
  tokens"`) and Tomcat starts — not just that the migration file parses.
- **What Academy/Wallet need to do next** (not this repo's job): send
  `appContext` at `/auth/login` and `/auth/google`. Nothing here assumes
  they've done so yet — that's exactly what `fromRequestValue`'s
  null-is-allowed handling is for.

Not done in this pass: §3/§4 (package restructuring, ArchUnit boundary
test) — still pending, per §0/§9.

## 11. `simulated_portfolio` context (2026-08-31, split plan Stage 2)

Academy's fictitious wallet — brand new context, no existing data to
migrate (the Financial Lab only ever wrote XP, never money — §2). Built
under the *current* package-by-layer-then-feature convention
(`application/simulatedportfolio/`, `infrastructure/entity/`,
`infrastructure/controller/simulatedportfolio/`,
`infrastructure/repository/simulatedportfolio/`), same as everything else
pre-§3 restructuring — not yet under a `com.jf.PetApp.simulatedportfolio`
package, but laid out so that move is mechanical when §3 happens.

- **Schema/tables**: `simulated_portfolios` (one per user, unique
  `user_id`, `virtual_balance`/`initial_balance` numeric(19,2),
  `reset_at`), `simulated_positions` (unique per `(portfolio_id, ticker)`,
  quantity numeric(19,6) for fractional shares), `simulated_orders`
  (append-only ledger, `client_order_id` unique per portfolio and always
  populated — client-supplied or server-generated UUID — for idempotent
  retries). Created unqualified in `db/migration/V22`, moved into the
  `simulated_portfolio` Postgres schema in `db/migration-postgres/V23` —
  same two-step pattern V1→V20 used, just without years of existing data to
  worry about. `spring.flyway.schemas` in `application-prod.properties`
  updated to include it.
- **Endpoints** (`/api/v1/simulated-portfolios/**`, gated behind
  `hasAuthority(APP_CONTEXT_ACADEMY)` in `SecurityConfig`, same pattern as
  academy/learning/lab): `GET /me` (lazy get-or-create summary + computed
  allocation%), `POST /orders` (buy/sell at the current reference quote,
  never a client-supplied price), `GET /orders` (history), `POST /reset`
  (wipes positions/orders, restores initial balance — requires
  `confirm: true`, validated by both `@AssertTrue` and the use case).
  Added afterward, for Stage 3's order-placement UI: `GET /quotes/search`
  and `GET /quotes/{ticker}` — Academy has no reachable equivalent of
  `/api/investments/search`/`/quote/{ticker}` (Wallet-only), so these two
  thin passthroughs onto the same `ExternalInvestmentApiPort` let it search
  tickers and preview a price before confirming an order.
- **Reference pricing**: reuses `ExternalInvestmentApiPort`
  (`application.investment.port`) for quotes — a deliberate, narrow,
  documented exception to the simulated/real boundary (public market data,
  not portfolio state). Enforced everywhere else by
  `SimulatedPortfolioBoundaryTest` (new ArchUnit test, `archunit-junit5`
  added as a test-scope dependency): no class under
  `application.simulatedportfolio`/`infrastructure.*.simulatedportfolio`
  may depend on `application.investment`/`InvestmentJpaEntity`/
  `FinanceJpaEntity`/etc, and vice versa, except that one port + its DTO.
  This is the §4 dependency-rule proposal, scoped to the one boundary that
  already needed it rather than waiting for full package restructuring.
- **`onboarding`/`InvestorProfile` → `identity`** decision (§3/§7) is also
  applied in this pass — see the inline note in §3 above.
- **Verified**: full `mvn test`, 874/874 (823 existing + 51 new — repository
  adapter `@DataJpaTest`, use-case unit tests including the buy/sell/reset/
  idempotency paths, controller `@WebMvcTest`, 2 new
  `SecurityConfigTest` authorization cases, 2 new ArchUnit rules). Full
  Spring Boot context boot smoke test still green.
- **Not done in this pass**: nothing on the Academy or Wallet Flutter apps
  yet — this context exists only in the backend. Consuming it (replacing
  Academy's current real-portfolio UI with this simulated one) is Stage 3
  of the split plan. No live-quote enrichment on position market value in
  `GET /me` yet (only cost-basis-derived numbers) — deferred as a
  non-blocking nice-to-have alongside the Stage 3 UI work.

## 12. `real_portfolio` precision migration — `double` → `BigDecimal` (2026-08-31, Stage 4)

`jf_investments.quantity`/`purchase_price` were `double precision` — real
money must never use binary floating point, per the standing rule that
real_portfolio calculations are never allowed to accumulate the rounding
error `double` compounds across many lots and achievement-threshold checks
(`portfolio_10k`/`portfolio_50k`/`positive_return` etc. compare a computed
`currentValue`/`totalGain` against a fixed threshold — a `double` sum across
enough lots can drift onto the wrong side of that threshold).

**What changed**, everywhere quantity/price/derived money flows through the
real-portfolio "ledger chain":
- `core/domain/Investment` (quantity, purchasePrice), `InvestmentJpaEntity`
  (`@Column(precision = 19, scale = 6)` for quantity, `scale = 2` for price).
- `V24__investment_precision.sql` — `ALTER TABLE jf_investments ALTER COLUMN
  ... SET DATA TYPE numeric(...) USING ...::numeric(...)`. Verified against
  this project's actual H2 2.4.240 (empirically, via a throwaway JDBC
  script) that this exact syntax — `SET DATA TYPE ... USING` — is accepted
  by both H2 and PostgreSQL, unlike H2's no-`USING` form or Postgres's bare
  `TYPE` keyword, which aren't mutually compatible in one statement. No
  rows dropped; every existing value cast in place.
- `ConfigureInvestmentCommand`, `AssetRegistrationDto`, `InvestmentRepositoryAdapter`
  (pass-through — no arithmetic, just wider types).
- `UserPositionCalculator` — full rewrite: average price, invested/current
  value, unrealized gain/%, portfolio weight, all `BigDecimal`, scale 2,
  `RoundingMode.HALF_UP`, matching simulated_portfolio's own convention.
- `UserPositionDTO`, `InvestmentLotDTO`, `PortfolioSummaryDTO`,
  `AllocationSliceDTO`, `PortfolioHistoryPointDTO` — all money/quantity
  fields converted; their use cases (`GetPortfolioHoldingsUseCaseImpl`,
  `GetPortfolioSummaryUseCaseImpl`, `GetPortfolioAllocationUseCaseImpl`,
  `GetPortfolioHistoryUseCaseImpl`, `GetAssetDetailsUseCaseImpl`) rewritten
  to match. `GetPortfolioHistoryUseCaseImpl`'s day-by-day interpolation
  fraction (`progress`, 0..1) deliberately stays `double` — it's a pure
  weight, not itself a money value; only its product with `BigDecimal`
  prices carries precision weight.
- `AchievementContext` (currentValue, totalGain, monthly/annual passive
  income estimate) and `AchievementCatalog`'s threshold predicates
  (`>`/`>=` → `.compareTo(...)`), since achievement qualification reads
  directly off this chain.

**Deliberately left `Double`, out of scope for this pass** (still a real,
tracked precision gap, but a separate calculation chain — converting it
would have roughly doubled this change's size for a lower-stakes surface):
- `DividendDTO`/`DividendRadarEntryDTO` and `GetDividendRadarUseCaseImpl`
  (Dividend Radar) — confirmed provider payment history, not the
  position/gain ledger achievements key off.
- `AssetDetailsResponseDTO`'s ~40 external market-fundamentals fields
  (P/E, ROE, margins, 52-week range, volumes, etc.) — raw pass-through
  display data from the provider, never persisted, never used in a
  balance-affecting calculation this backend owns.

**Verified**: full `mvn test`, 877/877 green (all pre-existing behavior
preserved, confirmed by running the untouched test suite through every
step of this migration rather than rewriting expectations blind).

## 13. Real B3/brokerage sync — preparatory architecture only (2026-08-31, Stage 4)

Audited what already exists before building anything: `BrapiInvestmentApiClient`
is the only external investment integration in this codebase, and it is
public **market-data quotes only** — no B3 endpoint, SDK, official contract,
token, or credential exists anywhere in this project, and none is invented
here. `jf_investments` is, and remains, entirely user-declared (manual
entry via `POST /api/investments/configure`, a full replace) — there is no
existing "sync from a real account" capability to consolidate; this is net
new, purely architectural work.

**What was built** — the exact shape the user's spec asked for when no
legitimate integration exists yet:
- `RealPortfolioSyncPort` (domain port) + `ExternalPositionDTO` (provider-
  agnostic internal shape: ticker/quantity/averagePrice/asOf — no
  provider-specific field ever crosses this boundary).
- `B3RealPortfolioSyncAdapter` — the only implementation, and a
  permanently-disabled one: `isEnabled()` requires both
  `app.b3-sync.enabled=true` and a non-blank `api.b3.token`; neither is set
  in any environment (`application.properties`, both blank/false by
  default, no `.env`/prod override exists). `fetchPositions(...)` throws
  rather than fabricate data if ever called while disabled. No credential
  of any kind is committed.
- `RealPortfolioSyncLogRepositoryPort`/`RealPortfolioSyncLogJpaEntity`
  (`real_portfolio_sync_log`, `real_portfolio` schema, migrations
  V25/V26 — same unqualified-then-schema-move pattern as V22/V23) — an
  audit row for **every** sync attempt, including `DISABLED` ones, unique
  per `(user_email, provider, idempotency_key)`.
- `SyncRealPortfolioUseCaseImpl` — idempotent (same shape as
  `XpLedgerService`/`PlaceSimulatedOrderUseCaseImpl`: a repeated
  idempotency key returns the already-logged outcome, never re-runs),
  handles provider unavailability (`FAILED`, never propagated as a 500),
  and treats "not configured" (`DISABLED`) as a normal 200 result, never an
  error — `POST /api/investments/sync` (already `APP_CONTEXT_WALLET`-gated
  by the existing blanket `/api/investments/**` rule, no `SecurityConfig`
  change needed).
- Deliberately **not** built: any reconciliation of a successful fetch's
  positions into `jf_investments` (replace vs merge vs flag-conflicts is a
  real product decision that needs an actual provider contract to design
  against — the successful-fetch path today only logs a position count).
  This path is structurally correct but practically unreachable in every
  real environment, by design, until real B3 credentials/contracts exist.

**Verified**: full `mvn test`, 893/893 green, including a disabled-adapter
unit test suite that pins `isEnabled()` false under every partial-config
combination (flag-only, token-only, neither) so a future config typo can't
silently start reporting "enabled" with nothing real behind it.

## 14. Pet/XP/Mentor context separation (2026-08-31, split plan Stage 6)

Audit-first, per the plan's methodology: before touching code, audited every
existing Pet/XP/Mentor path for a Wallet↔Academy content leak. Findings and
fixes below.

### Finding 1 (already compliant, no change needed): XP allow-list

`XpEventType` (`core/domain/gamification/XpEventType.java`) already only
recognizes `LESSON_COMPLETED`, `MODULE_COMPLETED`, `SIMULATOR_COMPLETED` —
`XpLedgerService.grantXp` is the single place XP is ever written, and
nothing outside those three call sites (`CompleteLessonUseCaseImpl`,
`CompleteSimulatorUseCaseImpl`) invokes it. `AchievementCatalog` — the only
other place that could plausibly grant XP from wealth/profit — has every
wealth-tied definition (`positive_return`, `portfolio_10k`, `portfolio_50k`,
`dividend_hunter`, `first_dividend`) hardcoded to `0` XP, each with an
explicit `DECISION-014`/`DECISION-027` comment. This was already correct
before Stage 6; verified, not changed.

### Finding 2 (already compliant by design, documented as intentional): canonical Pet

`Pet` (`core/domain/Pet.java`) is one record per user — no per-app-context
split. Confirmed this is the *intended* shape, not a gap: the pet is meant
to be a single cross-app companion (consistent with both apps' still-`
coming soon`-disabled `WalletBridgeCta`/`AcademyBridgeCta` cross-promotion),
and per Finding 1 its XP is already wealth-safe, so a Wallet session seeing
XP the user earned in Academy is by design, not a leak. `/api/pets/**` and
`/api/v1/gamification/**` stay unauthenticated-by-context (any authenticated
session may reach them) — see the updated comment in `SecurityConfig`.

### Finding 3 (live leak, fixed): Mentor mixed real and Academy content in one prompt

`GetMentorReplyUseCaseImpl` unconditionally called **both**
`GetPortfolioSummaryUseCase`/`GetPortfolioAllocationUseCase` (real money)
**and** `GetLearningProgressUseCase`/`GetAcademyCatalogUseCase` (Academy
lessons) on every `/api/mentor/chat` call, regardless of which app the
session belonged to — `MentorSystemPromptBuilder.build(...)` then wove both
into a single system prompt. A Wallet (real-money) user's Mentor
conversation could reference "Academy progress: level N..." and lesson
titles; an Academy user's conversation could reference real portfolio
numbers. `/api/mentor/**` also had no `app_context` gate at all in
`SecurityConfig`, so a token with no resolvable context (or the wrong one)
was served anyway. This directly violated the split brief's "Wallet must
never contain... Academy's internal Mentor implementation."

Fixed:

- **`MentorSystemPromptBuilder`** split into `buildForWallet(...)` (real
  portfolio + pet only — no parameter through which Academy data could
  reach it) and `buildForAcademy(...)` (simulated portfolio + pet +
  learning progress only — no real-portfolio parameter). The simulated
  portfolio block is always framed as "virtual money, NOT real" so the
  model's own language reinforces the app's on-screen disclaimer.
- **`GetMentorReplyUseCaseImpl`** takes a new `AppContextEnum appContext`
  parameter and branches: `ACADEMY` → simulated portfolio (new
  `GetSimulatedPortfolioUseCase` dependency) + learning progress;
  everything else, **including `null`** → the Wallet path. Whitelisting
  `ACADEMY` explicitly (rather than blacklisting `WALLET`) means an
  ambiguous/legacy session defaults to never seeing Academy content.
- **`SecurityConfig`**: `/api/mentor/**` now requires
  `hasAnyAuthority(APP_CONTEXT_WALLET, APP_CONTEXT_ACADEMY)` — a session
  with no resolvable context can no longer reach Mentor at all, since the
  use case has no safe default context to serve it under.
- **`SecurityUtils.getCurrentAppContext()`** (new) — reverse-looks-up the
  `APP_CONTEXT_*` granted authority `JwtAuthenticationFilter` already
  stamps onto the `Authentication`, via a new `AppContextEnum.fromAuthority(...)`.
- **Mentor conversations are now app_context-scoped**: `jf_mentor_conversations`
  gained a nullable `app_context` column (migration V27, same
  nullable/unqualified pattern as V21's refresh-token claim). `MentorConversation`,
  its JPA entity, and every read/write path (`create`, `findAllByUser`,
  `findByIdAndUser`) now filter on it — a Wallet session can't list, read,
  rename, or delete an Academy conversation (or vice versa) even by
  guessing its id; it 404s exactly as if it didn't exist. A conversation
  created before this migration (`app_context IS NULL`) is not retroactively
  assigned to either side — it simply becomes unreachable through these
  endpoints, since its content may already mix both (the pre-fix bug this
  migration exists to close).

### Finding 4 (related leak, fixed while auditing the same context boundary): Missions/Achievements had no context gate either

`/api/v1/missions/**` (Academy-only learning-quest content) and
`/api/v1/achievements/**` (Wallet-only wealth-threshold badges, evaluated
against the real portfolio) had no `SecurityConfig` gate — reachable by any
authenticated session regardless of context, same class of gap as Mentor's,
just not yet exploited by either Flutter app's current UI (Academy's own
real-money `PortfolioScreen`/`AchievementsSection` code path is already
known-unreachable dead code per Stage 3/5's audits). Fixed by adding
`/api/v1/missions/**` to the existing `ACADEMY`-only matcher group and a new
`/api/v1/achievements/**` → `WALLET`-only matcher, mirroring
`/api/investments/**`'s reasoning.

**Verified**: full `mvn test`, 918/918 green, including new anti-leak tests
(`GetMentorReplyUseCaseImplTest`: wallet context never touches any
Academy/simulated-portfolio use case and vice versa, null context takes the
Wallet-safe path, system prompts never cross-contaminate; a new
`MentorConversationRepositoryAdapterTest` case for cross-context id lookups;
new `SecurityConfigTest` end-to-end cases for the Mentor/Missions/Achievements
gates).

**Not done in this stage** (tracked as debt, revisit only if it becomes a
real product need): no UI/API to let a user see *which* app context a
Mentor conversation belongs to, since each app's own conversation list is
already implicitly scoped by the JWT it authenticates with.
