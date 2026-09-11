-- FR-WAL-005 requires validating currency and data origin before an asset
-- registration is confirmed, but the domain had no concept of either (see
-- the Wallet core-flow audit, 2026-09-10). Every existing and future write
-- path is manual entry against the only market Wallet supports (B3, BRL),
-- so both columns backfill to a fixed value rather than needing user input.
alter table jf_investments add column currency varchar(3);
alter table jf_investments add column origin varchar(20);

update jf_investments set currency = 'BRL', origin = 'MANUAL'
    where currency is null or origin is null;

alter table jf_investments alter column currency set not null;
alter table jf_investments alter column origin set not null;
