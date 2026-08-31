-- Moves the simulated_portfolio tables into their own Postgres schema — same
-- metadata-only ALTER TABLE ... SET SCHEMA pattern as V20
-- (docs/BACKEND_MODULE_PLAN.md §5-6), just for a schema that didn't exist at
-- V20 time. Safe because these tables were only just created by V22 in this
-- same deploy — nothing else could already be pointing at them, unlike V20's
-- real_portfolio/education/etc. tables which held years of existing data.

create schema if not exists simulated_portfolio;

alter table simulated_portfolios set schema simulated_portfolio;
alter table simulated_positions set schema simulated_portfolio;
alter table simulated_orders set schema simulated_portfolio;
