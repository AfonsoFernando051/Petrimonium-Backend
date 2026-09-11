-- Two devices/sessions editing the same investment lot concurrently could
-- silently last-write-wins clobber each other with no signal to either user
-- (flagged in the Wallet core-flow audit, 2026-09-10). Added ahead of wiring
-- a per-lot edit UI into the Wallet client, not for API symmetry.
alter table jf_investments add column version integer not null default 0;
