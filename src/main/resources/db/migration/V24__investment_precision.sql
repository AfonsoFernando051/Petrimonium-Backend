-- jf_investments.quantity/purchase_price were `double precision` — a real-money
-- field must never use binary floating point (rounding error compounds across
-- many lots/achievement-threshold checks). Converts both to fixed-point
-- numeric, matching simulated_portfolio's own scale convention (quantity
-- scale 6 for fractional shares, price scale 2). No rows are dropped; every
-- existing value is cast in place.
--
-- `SET DATA TYPE ... USING` is deliberately used over the shorter
-- `ALTER COLUMN c newtype` — both H2 2.x and PostgreSQL accept this exact
-- syntax (verified against the project's own H2 2.4.240), unlike H2's
-- no-USING form and Postgres's `TYPE` keyword, which aren't mutually
-- compatible in one statement.
alter table jf_investments
    alter column quantity set data type numeric(19,6) using quantity::numeric(19,6);
alter table jf_investments
    alter column purchase_price set data type numeric(19,2) using purchase_price::numeric(19,2);
