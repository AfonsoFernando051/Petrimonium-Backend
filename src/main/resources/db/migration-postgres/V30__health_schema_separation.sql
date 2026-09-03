-- PostgreSQL-only half of V29: move the newly-created Health tables into an
-- isolated bounded-context schema. ALTER TABLE SET SCHEMA is metadata-only;
-- rows, indexes and cross-schema foreign keys are preserved.
create schema if not exists health;

alter table health_profiles set schema health;
alter table health_accounts set schema health;
alter table health_recurrences set schema health;
alter table health_cards set schema health;
alter table health_card_invoices set schema health;
alter table health_card_purchases set schema health;
alter table health_card_installments set schema health;
alter table health_transfers set schema health;
alter table health_transactions set schema health;
