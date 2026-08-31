-- Moves real_portfolio_sync_log into the already-existing real_portfolio
-- schema (created by V20) — same metadata-only ALTER TABLE ... SET SCHEMA
-- pattern as V20/V23. Safe because this table was only just created by V25
-- in this same deploy.
alter table real_portfolio_sync_log set schema real_portfolio;
