-- jf_investments records real money and had no created_at/updated_at at all,
-- unlike every other audited entity in the codebase (simulated_portfolios,
-- xp_events, mentor_conversations) — there was no way to reconstruct "what
-- changed and when" outside what the Flutter app infers client-side.
--
-- Backfilled to now() rather than left null: every existing row predates
-- this column, so "now" is the most honest available value (matches the
-- convention InvestmentRepositoryAdapter.toEntity will now set for every
-- future insert — see DEM-32).
alter table jf_investments add column created_at timestamp;
alter table jf_investments add column updated_at timestamp;

update jf_investments set created_at = current_timestamp, updated_at = current_timestamp
    where created_at is null;

alter table jf_investments alter column created_at set not null;
alter table jf_investments alter column updated_at set not null;
