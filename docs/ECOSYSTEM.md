# Ecosystem role — Petrimonium Backend

Status as of 2026-08-31: **audited only — no code changed yet.** A prior
Claude Code session ran this repo's onboarding prompt, produced the audit
below, asked one direct question, and the session ended without the user
responding. Nothing here has been confirmed or implemented — treat this as
a proposal awaiting your decision.

## Notion

Project workspace: [Petrimonium](https://app.notion.com/p/3d08bfdad90780c3a935c0054a11770d)
— product docs, the Atlas Técnico (what the system *is*, today, read by
architecture slice) and the Demandas/Correção de Bugs boards findings from
work here should be tracked against.

## The three repos

| Repo | Job | Money |
|---|---|---|
| **`petrimonium-backend`** (this repo) | Shared Spring Boot / PostgreSQL backend for both Flutter apps | N/A (data layer) |
| [`petrimonium-academy`](../../Petrimonium-Academy) | Financial education, simulated money, full gamification | Simulated only |
| [`petrimonium-wallet`](../../Petrimonium-Wallet) | Real investment management, trust-forward, behavior-based gamification only | Real |

Both apps are meant to share one identity/account graph and one Pet entity
through this backend, but must never leak real-money data into Academy-scoped
requests or simulated data into Wallet-scoped requests. This repo is the
intended enforcement point for that boundary — client-side app separation
alone isn't sufficient.

## Where this actually came from

This backend (`com.jf.PetApp`) currently serves **one** client — the shared
starting point both `petrimonium-academy` and `petrimonium-wallet` were
forked from ([`Invest-Game-V2`](../../Invest-Game-V2), still under
independent active development). There is no Academy/Wallet split anywhere
in this backend's code, config, or docs yet. The target bounded-context
architecture below is a **first construction**, not a refactor of an
existing boundary — `investment` + `lab` are the raw material to eventually
split into `real_portfolio` (Wallet) / `simulated_portfolio` (Academy).

## Audit findings (§0 of the original prompt)

**Structure**: single Maven module, one Spring Boot app, one JAR — not a
multi-module build. Organized as hexagonal layers (`application` / `core` /
`infrastructure` / `presentation`), feature-packaged within those layers.

Mapping onto the six target bounded contexts:

| Target context | What exists today | Where |
|---|---|---|
| `identity` | `auth` (login, Google login, register, refresh, password reset) + `User`/`RoleEnum` | `application/auth/**`, `core/domain/User.java`, `infrastructure/security/jwt/**` |
| `education` | `academy` (schools/modules/lessons/quizzes) + `learning` (progress) — Academy-only, clean today | `application/academy/**`, `application/learning/**` |
| `portfolio` | **Not split.** `investment` (`Investment`, `Finance.balance`, brapi.dev market data) reads as a *real* self-reported portfolio but has no real/simulated marker anywhere — no `type`/`isSimulated` field, no schema flag. `lab` (`SimulatorCatalog`) is explicitly a practice/simulator feature with fixed XP and no real financial data — the one context that's unambiguous today. | `application/investment/**`, `application/lab/**` |
| `gamification` | XP ledger, streaks, achievements, missions — all generic, no app-context scoping | `application/gamification/**` |
| `pet` | Single `Pet` domain/entity — config/status CRUD only, no behavior-signal ingestion API yet | `core/domain/Pet.java`, `application/pet/**` |
| `ai` | `mentor` — Anthropic-primary/Gemini-fallback chat, generic system prompt, not app-context-aware | `application/mentor/**`, `infrastructure/external/AnthropicChatClient.java` |

`onboarding` (investor-profile questionnaire) and `settings` (language
prefs) fall outside the six contexts — generic, no leakage risk either way.

### Real vs. simulated leakage risks (prioritized — none are live incidents yet)

1. **`investment` has no real/simulated marker at all** (`core/domain/Investment.java:11`,
   `Finance`). No evidence found of simulated data mixed into this table, but
   the domain model can't currently prove it either way — confirm before
   this becomes `real_portfolio`.
2. **`gamification` has no context scoping** — nothing violates the Wallet
   gamification rule yet because Wallet doesn't exist as a client, but
   there's also no allow-list mechanism to build the constraint on top of.
   From-scratch design, not a redesign.
3. **`pet` has no signal-ingestion API** — the price-data exclusion rule
   (Wallet's signal API must never accept raw price/return data) is easy to
   enforce here specifically *because* nothing exists yet to retrofit.
4. **`mentor`'s system-prompt builder is a single shared path**
   (`MentorSystemPromptBuilder`) — worth checking when Wallet lands, since a
   shared prompt-construction path is where a Wallet-context leak into an
   Academy conversation (or vice versa) would first appear.

### Auth / JWT

- **Claims today**: `sub` (the user's **email**, not `user_id`) + `role`
  (`ADMIN`/`USER`) only (`infrastructure/security/jwt/JwtTokenProvider.java:39-45`).
  No `app_context`. No `provisioning`/KYC claim. No scopes beyond the
  two-value role.
- **Enforcement**: one filter chain — `/auth/**` and `/actuator/health` are
  public, everything else just requires *any* authenticated JWT
  (`SecurityConfig.java:101`, `.anyRequest().authenticated()`). No
  per-endpoint scope checks anywhere; `@EnableMethodSecurity` is wired but
  unused — no `@PreAuthorize` exists in the codebase (confirmed by the
  code's own comment at `SecurityConfig.java:22-27`).
- **KYC/provisioning**: none modeled. `User` has no KYC/financial-provisioning
  fields.
- The full `user_id`/`app_context`/`provisioning` claims contract (see
  `petrimonium-academy`'s `docs/CROSS_REPO_CONTRACTS.md` proposal) is new
  claim design here, not a migration of existing claims — but switching
  `sub` from email to `user_id` is a real compatibility question for both
  Flutter repos if/when that lands.

### PostgreSQL schema

Single schema, no logical separation. Flyway-owned (`classpath:db/migration`),
19 versioned migrations (`V1`…`V19`) plus a dev-only seed set. Tables span
academy content, learning progress, gamification, mentor conversations,
investments, refresh tokens, users — all flat, no `academy.*`/`portfolio.*`
schema-per-context boundary.

### Deployment shape (relevant to the BFF decision)

Single Spring Boot fat JAR, single Docker image, no API gateway, no
`.github/` CI workflows present despite a `pom.xml` comment referencing
`backend-ci.yml` (that file doesn't exist in this repo — either CI lives
elsewhere or the comment is stale, worth checking). A blank slate for the
BFF-as-module vs. BFF-as-gateway decision — nothing today constrains it
either way.

## Update, 2026-08-31

Everything below this point is the original audit, left as-is for history.
Since then: schema-per-context landed (`docs/BACKEND_MODULE_PLAN.md` §5-6/§9),
and so did the JWT `app_context` claim + BFF enforcement on
`/api/investments/**` (Wallet-only) and `/api/v1/academy|learning|lab/**`
(Academy-only) — see `docs/BACKEND_MODULE_PLAN.md` §10 for what actually
landed, including the one place it went beyond the `app_context` claim as
originally specified in `petrimonium-academy/docs/CROSS_REPO_CONTRACTS.md`
(refresh never accepts a client-supplied context, only the one stored at
login). The gamification allow-list and Pet signal-ingestion API design are
still not done. Package restructuring (§3-4 of the module plan) is also
still not done — deliberately deferred in favor of the app_context/BFF work.

## Update, 2026-08-31 (part 2 — split execution begins)

The user confirmed the full 3-repo functional split described in the
per-repo prompts is now underway, following a 7-stage incremental plan.
Stage 1 (baseline/contracts) closed two items:

- **Live regression fixed**: neither Flutter app was sending `appContext` at
  login, so both were already getting 403 from this backend on
  `/api/investments/**` (Wallet) and `/api/v1/academy|learning|lab/**`
  (Academy) — the enforcement landed before the clients adopted it. Both
  apps' `AuthRemoteDataSource.login`/`loginWithGoogle` now send their fixed
  `appContext` (`'wallet'` / `'academy'`), confirmed via each repo's own
  test suite.
- **`onboarding`/`InvestorProfile` module ownership resolved**: `identity`,
  not `education` — see `docs/BACKEND_MODULE_PLAN.md` §3/§7. No code moved
  yet (package restructuring is still deferred per the note above), this
  only closes the open decision for when that restructuring happens.

Still not done, unchanged from the note above: gamification allow-list, Pet
signal-ingestion API, package restructuring. Next was Stage 2, a new
`simulated_portfolio` context/schema for Academy's virtual wallet — see the
update immediately below.

## Update, 2026-08-31 (part 3 — Stage 2: `simulated_portfolio` built)

Stage 2 of the split plan is done. Full detail in
`docs/BACKEND_MODULE_PLAN.md` §11 — summary: new `simulated_portfolio`
Postgres schema and three tables (`simulated_portfolios`,
`simulated_positions`, `simulated_orders`), new
`/api/v1/simulated-portfolios/**` endpoints gated behind
`APP_CONTEXT_ACADEMY`, buy/sell executed at the real reference quote (never
a client-supplied price) via the existing `ExternalInvestmentApiPort`, and
a new ArchUnit test (`SimulatedPortfolioBoundaryTest`) enforcing that this
context and `real_portfolio`/`investment` never depend on each other,
except that one shared read-only quote port. No `real_portfolio` code or
data touched. Full suite: 874/874. This context exists only in the backend
so far — no Flutter app consumes it yet; that's Stage 3.

## Open question, never answered

The prior session stopped after the audit and asked directly: proceed to
the module-boundary + schema-per-context proposal (task 2 of the original
prompt), or weigh in first on the `investment` → `real_portfolio` real/
simulated question, since that's the one place genuine ambiguity was found
in the current data model? Both are resolved now — see the update above and
`docs/BACKEND_MODULE_PLAN.md` §2.

## What hasn't been done (as of the original audit — see the update above)

Everything except the audit. No module boundary plan, no BFF routing plan,
no gamification API allow-list, no Pet signal-ingestion API design, no JWT
claims contract draft.

## Update, 2026-08-31 (Stage 6: Pet/XP/Mentor context separation)

Stage 6 of the split plan is done — full detail in
`docs/BACKEND_MODULE_PLAN.md` §14. Summary: audited every Pet/XP/Mentor path
for a Wallet↔Academy leak. XP was already allow-listed to learning/practice
events only (no code change needed) and the canonical single-Pet-per-user
design was confirmed intentional (the pet is meant to be one cross-app
companion). Found and fixed a live leak: `/api/mentor/chat` built one system
prompt that unconditionally mixed real portfolio data with Academy lesson
progress regardless of which app the session belonged to, and had no
app_context gate at all. `MentorSystemPromptBuilder` is now split into
context-specific `buildForWallet`/`buildForAcademy` entry points with no
parameter through which the other context's data could reach them; Mentor
conversations are now app_context-scoped end to end (new `app_context`
column, migration V27); `/api/mentor/**` now requires a resolvable
app_context. Also closed the same class of gap on `/api/v1/missions/**`
(now Academy-only) and `/api/v1/achievements/**` (now Wallet-only), found
while auditing the same boundary. Full suite: 918/918.
