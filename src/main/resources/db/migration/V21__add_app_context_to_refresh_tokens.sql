-- Carries the app_context claim (docs/BACKEND_MODULE_PLAN.md task 6 / CROSS_REPO_CONTRACTS.md
-- §1) across refresh-token rotation, so a session's app scope is fixed at login and can't be
-- changed by presenting a refresh token with a different value later (RefreshTokenUseCaseImpl
-- always reuses the stored value, never a client-supplied one).
--
-- Nullable and unqualified (no schema prefix) for the same reason V18 is: this table lives in
-- the identity schema in Postgres and unqualified in H2, and both resolve it via search_path
-- (see docs/BACKEND_MODULE_PLAN.md §9). Existing rows get NULL — a pre-existing session simply
-- carries no app_context until it re-authenticates.

alter table jf_refresh_tokens add column app_context varchar(20);
